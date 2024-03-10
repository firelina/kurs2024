package ru.avm.kurs.model;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class ModelConsumerImpl implements ModelConsumer{
    private final String title;
    @Override
    public void consume(ModelAgent agent) {
        log.info(title + " agent: " + agent.getTitle() + " param: " + agent.getState());
    }
}
