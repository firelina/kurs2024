package ru.avm.kurs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.avm.kurs.controller.dto.ModelAgentInitDTO;
import ru.avm.kurs.controller.dto.ModelConsumerInitDTO;
import ru.avm.kurs.controller.dto.StartModelDTO;
import ru.avm.kurs.exception.ModelException;
import ru.avm.kurs.model.ModelAgent;
import ru.avm.kurs.model.ModelConsumer;
import ru.avm.kurs.model.ModelConsumerImpl;
import ru.avm.kurs.stat.BankomatStat;
import ru.avm.kurs.stat.ClerkStat;
import ru.avm.kurs.stat.ModelStatistics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static ru.avm.kurs.util.ModelUtil.randomBetween;

@Component
@Slf4j
public class ModelServiceImpl implements ModelService{
    private final ModelQueueConfig modelQueueConfig;
    private ModelQueueService commonQueue;
    private ModelQueueService bankomatQueue;
    private ModelQueueService clerkQueue;
    private final DelayServiceConfig delayServiceConfig;
    private DelayService commonDelay;
    private DelayService bankomatDelay;
    private DelayService clerkDelay;
    private final Lock lock = new ReentrantLock();
    private final Lock clerkBeforeLock = new ReentrantLock();
    private final Lock clerkAfterLock = new ReentrantLock();
    private final Lock bankomatBeforeLock = new ReentrantLock();
    private final Lock bankomatAfterLock = new ReentrantLock();
    private final ConcurrentHashMap<String, Thread> corelation = new ConcurrentHashMap<>();
    private ModelStatistics modelStatistics;
    private Integer bankomatTimeLimit;
    private Integer clerkTimeLimit;
    @Autowired
    public ModelServiceImpl(ModelQueueConfig modelQueueConfig, DelayServiceConfig delayServiceConfig) {
        this.modelQueueConfig = modelQueueConfig;
        this.delayServiceConfig = delayServiceConfig;
    }

    @Override
    public String startModel(StartModelDTO initData) {
        lock.lock();
        if(corelation.isEmpty()) {
            if(Objects.isNull(initData)) {
                initData = new StartModelDTO();
                initData.setAgent(new ModelAgentInitDTO("common", 1, 0, 2));
                initData.setConsumers(Arrays.asList(
                        ModelConsumerInitDTO.builder().state(1).count(1).prefTitle("bankomat").firstDelay(4).secondDelay(0).thirdDelay(6).timeLimit(10).build(),
                        ModelConsumerInitDTO.builder().state(2).count(5).prefTitle("clerk").firstDelay(3).secondDelay(0).thirdDelay(6).timeLimit(15).build()
                ));
            }
            commonQueue = modelQueueConfig.commonQueue(1, initData.getAgent().getPrefTitle());
            commonDelay = delayServiceConfig.commonDelay(initData.getAgent().getFirstDelay(), initData.getAgent().getSecondDelay(), initData.getAgent().getThirdDelay());
            this.modelStatistics = new ModelStatistics();
            initData.getConsumers().forEach(i -> {
                if (i.getState() == 1) {
                    bankomatQueue = modelQueueConfig.bankomatQueue(i.getCount(), i.getPrefTitle());
                    bankomatDelay = delayServiceConfig.bankomatDelay(i.getFirstDelay(), i.getSecondDelay(), i.getThirdDelay());
                    bankomatTimeLimit = i.getTimeLimit();
                    for (int j = 0; j < i.getCount(); j++) {
                        modelStatistics.getBankomatMap().put(i.getPrefTitle() + "-" + j, new BankomatStat(i.getPrefTitle() + "-" + j, 0, false));
                    }
                }
                if (i.getState() == 2) {
                    clerkQueue = modelQueueConfig.clerkQueue(i.getCount(), i.getPrefTitle());
                    clerkDelay = delayServiceConfig.clerktDelay(i.getFirstDelay(), i.getSecondDelay(), i.getThirdDelay());
                    clerkTimeLimit = i.getTimeLimit();
                    for (int j = 0; j < i.getCount(); j++) {
                        modelStatistics.getClerkMap().put(i.getPrefTitle() + "-" + j, new ClerkStat(i.getPrefTitle() + "-" + j, 0, false));
                    }
                }
            });

            Thread thread = new Thread(player());
            String guid = UUID.randomUUID().toString();
            corelation.put(guid, thread);
            thread.start();
        }
        lock.unlock();
        return corelation.keys().nextElement();
    }
    @Override
    public String stopModel(String guidModel) {
        corelation.get(guidModel).interrupt();
        corelation.remove(guidModel);
        commonQueue.getBuffer().clear();
        commonQueue.getExecutor().shutdownNow();
        bankomatQueue.getBuffer().clear();
        bankomatQueue.getExecutor().shutdownNow();
        clerkQueue.getBuffer().clear();
        clerkQueue.getExecutor().shutdownNow();
        return guidModel;
    }

    @Override
    public ModelStatistics getStats() {
        modelStatistics.setBankomats(modelStatistics.getBankomatMap().entrySet().stream()
                .map(i -> new BankomatStat(i.getKey(), i.getValue().getServiced(), i.getValue().getIsBusy()))
                .sorted((obj1, obj2) -> obj1.getTitle().compareToIgnoreCase(obj2.getTitle()))
                .collect(Collectors.toList()));
        modelStatistics.setClerks(modelStatistics.getClerkMap().entrySet().stream()
                .map(i -> new ClerkStat(i.getKey(), i.getValue().getServiced(), i.getValue().getIsBusy()))
                .sorted((obj1, obj2) -> obj1.getTitle().compareToIgnoreCase(obj2.getTitle()))
                .collect(Collectors.toList()));
        return modelStatistics;
    }

    private Runnable player(){
        return () -> {
            int i = 0;
            while(!Thread.currentThread().isInterrupted() && i < 100) {
                ModelAgent modelAgent = new ModelAgent("agent " + (i + 1), randomBetween(1, 2));
                commonDelay.delay();
                commonQueue.getExecutor().execute(commonProducer(modelAgent));
                commonQueue.getExecutor().submit(commonConsumer());
                i++;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new ModelException(e.getMessage());
            } finally {
                corelation.clear();
                commonQueue.getExecutor().shutdown();
                bankomatQueue.getExecutor().shutdown();
                clerkQueue.getExecutor().shutdown();
            }
        };
    }

    private Runnable commonProducer(ModelAgent actor){
        return () -> {
            try {
                commonQueue.getBuffer().put(actor);
            } catch (InterruptedException e) {
                throw new ModelException(e.getMessage());
            }
        };
    }
    private Runnable commonConsumer(){
        return () -> {
            try {
                ModelAgent commonActor = commonQueue.getBuffer().take();
                if(commonActor.getState() == 1){
                    bankomatQueue.getExecutor().execute(bankomatProducer(commonActor));
                    bankomatQueue.getExecutor().submit(bankomatConsumer());
                }
                else {
                    clerkQueue.getExecutor().execute(clerkProducer(commonActor));
                    clerkQueue.getExecutor().submit(clerkConsumer());
                }
            } catch (InterruptedException e) {
                throw new ModelException(e.getMessage());
            }
        };
    }
    private Runnable bankomatProducer(ModelAgent actor){
        return () -> {
            try {
                bankomatQueue.getBuffer().put(actor);
                modelStatistics.setSizeBankomatQueue(((ThreadPoolExecutor) bankomatQueue.getExecutor()).getQueue().size());
            } catch (InterruptedException e) {
                throw new ModelException(e.getMessage());
            }
        };
    }
    private Runnable bankomatConsumer(){
        return () -> {
            try {
                bankomatBeforeLock.lock();
                ModelAgent bankomatActor = bankomatQueue.getBuffer().take();
                if((bankomatTimeLimit - ((System.currentTimeMillis() - bankomatActor.getStartTime())/1000)) <=0){
                    modelStatistics.setBankomatNotServed(modelStatistics.getBankomatNotServed() + 1);
                    modelStatistics.setSizeBankomatQueue(((ThreadPoolExecutor) bankomatQueue.getExecutor()).getQueue().size());
                    bankomatBeforeLock.unlock();
                    return;
                }
                ModelConsumer modelConsumer = new ModelConsumerImpl(Thread.currentThread().getName());
                modelConsumer.consume(bankomatActor);

                final BankomatStat bankomatBeforeStat = modelStatistics.getBankomatMap().getOrDefault(Thread.currentThread().getName(), new BankomatStat(Thread.currentThread().getName(), 0, true));
                bankomatBeforeStat.setIsBusy(true);
                modelStatistics.getBankomatMap().put(Thread.currentThread().getName(), bankomatBeforeStat);
                bankomatBeforeLock.unlock();

                bankomatDelay.delay();

                bankomatAfterLock.lock();
                final BankomatStat bankomatAfterStat = modelStatistics.getBankomatMap().get(Thread.currentThread().getName());
                bankomatAfterStat.setIsBusy(false);
                bankomatAfterStat.setServiced(bankomatAfterStat.getServiced() + 1);
                modelStatistics.getBankomatMap().put(Thread.currentThread().getName(), bankomatAfterStat);
                modelStatistics.setSizeBankomatQueue(((ThreadPoolExecutor) bankomatQueue.getExecutor()).getQueue().size());
                bankomatAfterLock.unlock();
            } catch (InterruptedException e) {
                throw new ModelException(e.getMessage());
            }
        };
    }
    private Runnable clerkProducer(ModelAgent actor){
        return () -> {
            try {
                clerkQueue.getBuffer().put(actor);
                modelStatistics.setSizeClerktQueue(((ThreadPoolExecutor) clerkQueue.getExecutor()).getQueue().size());
            } catch (InterruptedException e) {
                throw new ModelException(e.getMessage());
            }
        };
    }
    private Runnable clerkConsumer(){
        return () -> {
            try {
                clerkBeforeLock.lock();
                ModelAgent clerkActor = clerkQueue.getBuffer().take();
                if((clerkTimeLimit - ((System.currentTimeMillis() - clerkActor.getStartTime())/1000)) <= 0){
                    modelStatistics.setClerkNotServed(modelStatistics.getClerkNotServed() + 1);
                    modelStatistics.setSizeClerktQueue(((ThreadPoolExecutor) clerkQueue.getExecutor()).getQueue().size());
                    clerkBeforeLock.unlock();
                    return;
                }
                ModelConsumer modelConsumer = new ModelConsumerImpl(Thread.currentThread().getName());
                modelConsumer.consume(clerkActor);

                final ClerkStat clerkBeforeStat = modelStatistics.getClerkMap().getOrDefault(Thread.currentThread().getName(), new ClerkStat(Thread.currentThread().getName(), 0, true));
                clerkBeforeStat.setIsBusy(true);
                modelStatistics.getClerkMap().put(Thread.currentThread().getName(), clerkBeforeStat);
                clerkBeforeLock.unlock();

                clerkDelay.delay();
                clerkAfterLock.lock();
                final ClerkStat clerkAfterStat = modelStatistics.getClerkMap().get(Thread.currentThread().getName());
                clerkAfterStat.setIsBusy(false);
                clerkAfterStat.setServiced(clerkAfterStat.getServiced() + 1);
                modelStatistics.getClerkMap().put(Thread.currentThread().getName(), clerkAfterStat);
                modelStatistics.setSizeClerktQueue(((ThreadPoolExecutor) clerkQueue.getExecutor()).getQueue().size());
                clerkAfterLock.unlock();
            } catch (InterruptedException e) {
                throw new ModelException(e.getMessage());
            }
        };
    }
}
