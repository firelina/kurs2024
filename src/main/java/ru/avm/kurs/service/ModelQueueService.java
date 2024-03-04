package ru.avm.kurs.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import ru.avm.kurs.model.ModelAgent;

import java.util.Objects;
import java.util.concurrent.*;


@Getter
public class ModelQueueService {
    private int countThread;
    private final BlockingQueue<ModelAgent> buffer = new LinkedBlockingQueue<>();
    private ExecutorService executor;

    public ModelQueueService(int countThread, String title) {
        this.countThread = countThread;
        if(Objects.isNull(title))
            executor = Executors.newFixedThreadPool(countThread);
        else {
            final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                    .setNameFormat(title + "-%d")
                    .build();
            executor = Executors.newFixedThreadPool(countThread, threadFactory);
        }
    }

    @PreDestroy
    public void destroy(){
//        executor.shutdown();
    }
}
