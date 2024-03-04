package ru.avm.kurs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.avm.kurs.controller.dto.StartModelDTO;
import ru.avm.kurs.exception.ModelException;
import ru.avm.kurs.model.ModelAgent;
import ru.avm.kurs.model.ModelConsumer;
import ru.avm.kurs.model.ModelConsumerImpl;
import ru.avm.kurs.stat.BankomatStat;
import ru.avm.kurs.stat.ClerkStat;
import ru.avm.kurs.stat.ModelStatistics;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ModelServiceImpl implements ModelService{
    private final ModelQueueConfig modelQueueConfig;
    private ModelQueueService commonQueue;
    private ModelQueueService bankomatQueue;
    private ModelQueueService clerkQueue;
    private final Lock lock = new ReentrantLock();
    private final ConcurrentHashMap<String, Thread> corelation = new ConcurrentHashMap<>();
    private ModelStatistics modelStatistics;
    @Autowired
    public ModelServiceImpl(ModelQueueConfig modelQueueConfig) {
        this.modelQueueConfig = modelQueueConfig;
//        commonQueue = modelQueueConfig.commonQueue();
//        bankomatQueue = modelQueueConfig.bankomatQueue();
//        clerkQueue = modelQueueConfig.clerkQueue();
    }

    @Override
    public String startModel(StartModelDTO initData) {
        lock.lock();
        if(corelation.isEmpty()) {
            modelStatistics = new ModelStatistics();
            commonQueue = modelQueueConfig.commonQueue();
            bankomatQueue = modelQueueConfig.bankomatQueue();
            clerkQueue = modelQueueConfig.clerkQueue();
            log.info(commonQueue.toString());
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
        modelStatistics.setBankomats(modelStatistics.getBankomatMap().entrySet().stream().map(i -> new BankomatStat(i.getKey(), i.getValue())).collect(Collectors.toList()));
        modelStatistics.setClerks(modelStatistics.getClerkMap().entrySet().stream().map(i -> new ClerkStat(i.getKey(), i.getValue())).collect(Collectors.toList()));
        return modelStatistics;
    }

    private Runnable player(){
        return () -> {
            int i = 0;
            while(!Thread.currentThread().isInterrupted() && i < 100) {
//                for (int i = 0; i < 100; i++) {
                    ModelAgent modelAgent = new ModelAgent("actor" + (i + 1), randomBetween(1, 2));
                    try {
                        Thread.sleep(100 * randomBetween(0, 10));
                        commonQueue.getExecutor().execute(commonProducer(modelAgent));
                        commonQueue.getExecutor().submit(commonConsumer());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new ModelException(e.getMessage());
                    }
                    i++;
//                }
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
            } catch (InterruptedException e) {
                throw new ModelException(e.getMessage());
            }
        };
    }
    private Runnable bankomatConsumer(){
        return () -> {
            try {
                ModelAgent bankomatActor = bankomatQueue.getBuffer().take();
                ModelConsumer modelConsumer = new ModelConsumerImpl("bankomat");
                modelConsumer.consume(bankomatActor);
                Thread.sleep(1000 * randomBetween(1, 2));
                modelStatistics.getBankomatMap().put(Thread.currentThread().getName(), modelStatistics.getBankomatMap().getOrDefault(Thread.currentThread().getName(), 0)+1);
            } catch (InterruptedException e) {
                throw new ModelException(e.getMessage());
            }
        };
    }
    private Runnable clerkProducer(ModelAgent actor){
        return () -> {
            try {
                clerkQueue.getBuffer().put(actor);
            } catch (InterruptedException e) {
                throw new ModelException(e.getMessage());
            }
        };
    }
    private Runnable clerkConsumer(){
        return () -> {
            try {
                ModelAgent clerkActor = clerkQueue.getBuffer().take();
                ModelConsumer modelConsumer = new ModelConsumerImpl("clerk");
                modelConsumer.consume(clerkActor);
                Thread.sleep(1000 * randomBetween(2, 5));
                modelStatistics.getClerkMap().put(Thread.currentThread().getName(), modelStatistics.getClerkMap().getOrDefault(Thread.currentThread().getName(), 0)+1);
            } catch (InterruptedException e) {
                throw new ModelException(e.getMessage());
            }
        };
    }

    private Integer randomBetween(int minimum, int maximum){
        Random rn = new Random();
        return rn.nextInt(maximum - minimum + 1) + minimum;
    }
}
