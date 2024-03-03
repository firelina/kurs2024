package ru.avm.kurs.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelQueueConfig {
    @Bean
    public ModelQueueService commonQueue(){
        return new ModelQueueService(1);
    }
    @Bean
    public ModelQueueService bankomatQueue(){
        return new ModelQueueService(2);
    }
    @Bean
    public ModelQueueService clerkQueue(){
        return new ModelQueueService(5);
    }
}
