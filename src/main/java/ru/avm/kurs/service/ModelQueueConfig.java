package ru.avm.kurs.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.function.Function;

@Configuration
public class ModelQueueConfig {
    @Bean(autowireCandidate = false)
    @Scope("request")
    public ModelQueueService commonQueue(Integer countThreads, String title){
        return new ModelQueueService(countThreads, title);
    }
    @Bean(autowireCandidate = false)
    @Scope("request")
    public ModelQueueService bankomatQueue(Integer countThreads, String title){
        return new ModelQueueService(countThreads, title);
    }
    @Bean(autowireCandidate = false)
    @Scope("request")
    public ModelQueueService clerkQueue(Integer countThreads, String title){
        return new ModelQueueService(countThreads, title);
    }
}
