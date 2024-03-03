package ru.avm.kurs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.avm.kurs.controller.dto.StartModelDTO;
import ru.avm.kurs.exception.ModelException;
import ru.avm.kurs.model.ModelActor;
import ru.avm.kurs.model.ModelConsumer;
import ru.avm.kurs.model.ModelConsumerImpl;

import java.util.Random;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService{
    private final ModelQueueService commonQueue;
    private final ModelQueueService bankomatQueue;
    private final ModelQueueService clerkQueue;
    @Override
    public String startModel(StartModelDTO initData) {
        for (int i = 0; i < 100; i++) {
            ModelActor modelActor = new ModelActor("actor" + (i+1), randomBetween(1, 2));
            try {
                Thread.sleep(100 * randomBetween(0, 10));
                commonQueue.getExecutor().execute(commonProducer(modelActor));
                commonQueue.getExecutor().submit(commonConsumer());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return UUID.randomUUID().toString();
    }

    @Override
    public String stopModel(String guidModel) {
        return guidModel;
    }

    private Runnable commonProducer(ModelActor actor){
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
                ModelActor commonActor = commonQueue.getBuffer().take();
                if(commonActor.getSomeParam() == 1){
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
    private Runnable bankomatProducer(ModelActor actor){
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
                ModelActor bankomatActor = bankomatQueue.getBuffer().take();
                ModelConsumer modelConsumer = new ModelConsumerImpl("bankomat");
                modelConsumer.consume(bankomatActor);
                Thread.sleep(1000 * randomBetween(1, 2));
            } catch (InterruptedException e) {
                throw new ModelException(e.getMessage());
            }
        };
    }
    private Runnable clerkProducer(ModelActor actor){
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
                ModelActor clerkActor = clerkQueue.getBuffer().take();
                ModelConsumer modelConsumer = new ModelConsumerImpl("clerk");
                modelConsumer.consume(clerkActor);
                Thread.sleep(1000 * randomBetween(2, 5));
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
