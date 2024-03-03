package ru.avm.kurs.service;

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.stereotype.Component;
import ru.avm.kurs.model.ModelActor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;


@Getter
public class ModelQueueService {
    private int countThread;
    private final BlockingQueue<ModelActor> buffer = new LinkedBlockingQueue<>();
    private ExecutorService executor;

    public ModelQueueService(int countThread) {
        this.countThread = countThread;
        executor = Executors.newFixedThreadPool(countThread);
    }

    @PreDestroy
    public void destroy(){
        executor.shutdown();
    }
}
