package ru.avm.kurs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.avm.kurs.controller.dto.ModelAgentInitDTO;
import ru.avm.kurs.controller.dto.ModelConsumerInitDTO;
import ru.avm.kurs.controller.dto.PetriAgentDTO;
import ru.avm.kurs.controller.dto.StartModelDTO;
import ru.avm.kurs.exception.ModelException;
import ru.avm.kurs.model.ModelAgent;
import ru.avm.kurs.model.ModelConsumer;
import ru.avm.kurs.model.ModelConsumerImpl;
import ru.avm.kurs.stat.SourceStat;
import ru.avm.kurs.stat.ModelStatistics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private Map<String, PetriAgentDTO> petris;
    @Autowired
    public ModelServiceImpl(ModelQueueConfig modelQueueConfig, DelayServiceConfig delayServiceConfig) {
        this.modelQueueConfig = modelQueueConfig;
        this.delayServiceConfig = delayServiceConfig;
    }

    @Override
    public String startModel(StartModelDTO initData) {
        lock.lock();
        if(corelation.isEmpty()) {
            petris = new ConcurrentHashMap();
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
                        modelStatistics.getBankomatMap().put(i.getPrefTitle() + "-" + j, new SourceStat(i.getPrefTitle() + "-" + j, 0, false));
                    }
                }
                if (i.getState() == 2) {
                    clerkQueue = modelQueueConfig.clerkQueue(i.getCount(), i.getPrefTitle());
                    clerkDelay = delayServiceConfig.clerktDelay(i.getFirstDelay(), i.getSecondDelay(), i.getThirdDelay());
                    clerkTimeLimit = i.getTimeLimit();
                    for (int j = 0; j < i.getCount(); j++) {
                        modelStatistics.getClerkMap().put(i.getPrefTitle() + "-" + j, new SourceStat(i.getPrefTitle() + "-" + j, 0, false));
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
                .map(i -> new SourceStat(i.getKey(), i.getValue().getServiced(), i.getValue().getIsBusy()))
                .sorted((obj1, obj2) -> obj1.getTitle().compareToIgnoreCase(obj2.getTitle()))
                .collect(Collectors.toList()));
        modelStatistics.setClerks(modelStatistics.getClerkMap().entrySet().stream()
                .map(i -> new SourceStat(i.getKey(), i.getValue().getServiced(), i.getValue().getIsBusy()))
                .sorted((obj1, obj2) -> obj1.getTitle().compareToIgnoreCase(obj2.getTitle()))
                .collect(Collectors.toList()));
        return modelStatistics;
    }

    @Override
    public List<PetriAgentDTO> getPetri() {
        return petris.entrySet().stream()
                .map(i -> i.getValue())
                .sorted(Comparator.comparingInt(obj -> Integer.valueOf(obj.getTitle().split(" ")[1]))
                )
                .collect(Collectors.toList());
    }

    private Runnable player(){
        return () -> {
            int i = 0;
            while(!Thread.currentThread().isInterrupted() && i < 100) {
                String title = "agent " + (i + 1);
                ModelAgent modelAgent = new ModelAgent(title, randomBetween(1, 2));

                petris.put(title, new PetriAgentDTO(title));
                PetriAgentDTO petri = petris.get(title);
                petri.getStates().add(1);
                petris.put(title, petri);

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

                PetriAgentDTO petri = petris.get(actor.getTitle());
                petri.getStates().add(2);
                petris.put(actor.getTitle(), petri);

            } catch (InterruptedException e) {
                throw new ModelException(e.getMessage());
            }
        };
    }
    private Runnable commonConsumer(){
        return () -> {
            try {
                ModelAgent commonActor = commonQueue.getBuffer().take();

                PetriAgentDTO petri = petris.get(commonActor.getTitle());
                petri.getStates().add(3);
                petris.put(commonActor.getTitle(), petri);

                if(commonActor.getState() == 1){

                    PetriAgentDTO petriB = petris.get(commonActor.getTitle());
                    petriB.getStates().add(4);
                    petris.put(commonActor.getTitle(), petriB);

                    bankomatQueue.getExecutor().execute(bankomatProducer(commonActor));
                    bankomatQueue.getExecutor().submit(bankomatConsumer());
                }
                else {
                    PetriAgentDTO petriC = petris.get(commonActor.getTitle());
                    petriC.getStates().add(9);
                    petris.put(commonActor.getTitle(), petriC);
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
                PetriAgentDTO petri = petris.get(actor.getTitle());
                petri.getStates().add(5);
                petris.put(actor.getTitle(), petri);
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
                ModelAgent agent = bankomatQueue.getBuffer().take();
                if((bankomatTimeLimit - ((System.currentTimeMillis() - agent.getStartTime())/1000)) <=0){
                    modelStatistics.setBankomatNotServed(modelStatistics.getBankomatNotServed() + 1);
                    modelStatistics.setSizeBankomatQueue(((ThreadPoolExecutor) bankomatQueue.getExecutor()).getQueue().size());

                    PetriAgentDTO petri = petris.get(agent.getTitle());
                    petri.getStates().add(6);
                    petris.put(agent.getTitle(), petri);

                    bankomatBeforeLock.unlock();
                    return;
                }

                PetriAgentDTO petri = petris.get(agent.getTitle());
                petri.getStates().add(7);
                petris.put(agent.getTitle(), petri);

                ModelConsumer modelConsumer = new ModelConsumerImpl(Thread.currentThread().getName());
                modelConsumer.consume(agent);

                final SourceStat bankomatBeforeStat = modelStatistics.getBankomatMap().getOrDefault(Thread.currentThread().getName(), new SourceStat(Thread.currentThread().getName(), 0, true));
                bankomatBeforeStat.setIsBusy(true);
                modelStatistics.getBankomatMap().put(Thread.currentThread().getName(), bankomatBeforeStat);
                bankomatBeforeLock.unlock();

                bankomatDelay.delay();

                bankomatAfterLock.lock();
                final SourceStat bankomatAfterStat = modelStatistics.getBankomatMap().get(Thread.currentThread().getName());
                bankomatAfterStat.setIsBusy(false);
                bankomatAfterStat.setServiced(bankomatAfterStat.getServiced() + 1);
                modelStatistics.getBankomatMap().put(Thread.currentThread().getName(), bankomatAfterStat);
                modelStatistics.setSizeBankomatQueue(((ThreadPoolExecutor) bankomatQueue.getExecutor()).getQueue().size());

                PetriAgentDTO petriAfterServiced = petris.get(agent.getTitle());
                petriAfterServiced.getStates().add(8);
                petris.put(agent.getTitle(), petriAfterServiced);

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

                PetriAgentDTO petri = petris.get(actor.getTitle());
                petri.getStates().add(10);
                petris.put(actor.getTitle(), petri);

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

                    PetriAgentDTO petri = petris.get(clerkActor.getTitle());
                    petri.getStates().add(11);
                    petris.put(clerkActor.getTitle(), petri);

                    clerkBeforeLock.unlock();
                    return;
                }

                PetriAgentDTO petri = petris.get(clerkActor.getTitle());
                petri.getStates().add(12);
                petris.put(clerkActor.getTitle(), petri);

                ModelConsumer modelConsumer = new ModelConsumerImpl(Thread.currentThread().getName());
                modelConsumer.consume(clerkActor);

                final SourceStat clerkBeforeStat = modelStatistics.getClerkMap().getOrDefault(Thread.currentThread().getName(), new SourceStat(Thread.currentThread().getName(), 0, true));
                clerkBeforeStat.setIsBusy(true);
                modelStatistics.getClerkMap().put(Thread.currentThread().getName(), clerkBeforeStat);
                clerkBeforeLock.unlock();

                clerkDelay.delay();
                clerkAfterLock.lock();
                final SourceStat clerkAfterStat = modelStatistics.getClerkMap().get(Thread.currentThread().getName());
                clerkAfterStat.setIsBusy(false);
                clerkAfterStat.setServiced(clerkAfterStat.getServiced() + 1);
                modelStatistics.getClerkMap().put(Thread.currentThread().getName(), clerkAfterStat);
                modelStatistics.setSizeClerktQueue(((ThreadPoolExecutor) clerkQueue.getExecutor()).getQueue().size());

                PetriAgentDTO petriAgentAfterServiced = petris.get(clerkActor.getTitle());
                petri.getStates().add(13);
                petris.put(clerkActor.getTitle(), petriAgentAfterServiced);

                clerkAfterLock.unlock();
            } catch (InterruptedException e) {
                throw new ModelException(e.getMessage());
            }
        };
    }
}
