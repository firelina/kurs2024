package ru.avm.kurs.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.function.Function;

@Configuration
public class ModelQueueConfig {
    @Bean(autowireCandidate = false)
    @Scope("request")
    public ModelQueueService commonQueue(){
        return new ModelQueueService(1, "common");
    }
    @Bean(autowireCandidate = false)
    @Scope("request")
    public ModelQueueService bankomatQueue(){
        return new ModelQueueService(2, "bankomat");
    }
    @Bean(autowireCandidate = false)
    @Scope("request")
    public ModelQueueService clerkQueue(){
        return new ModelQueueService(5, "clerk");
    }
}
