package ru.avm.kurs.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class DelayServiceConfig {
    @Bean(autowireCandidate = false)
    @Scope("request")
    public DelayService commonDelay(Integer first, Integer middle, Integer last){
        return new DelayServiceImpl(first, middle, last);
    }
    @Bean(autowireCandidate = false)
    @Scope("request")
    public DelayService bankomatDelay(Integer first, Integer middle, Integer last){
        return new DelayServiceImpl(first, middle, last);
    }
    @Bean(autowireCandidate = false)
    @Scope("request")
    public DelayService clerktDelay(Integer first, Integer middle, Integer last){
        return new DelayServiceImpl(first, middle, last);
    }
}
