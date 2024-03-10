package ru.avm.kurs.service;

import ru.avm.kurs.exception.ModelException;

import java.util.Objects;
import static ru.avm.kurs.util.ModelUtil.randomBetween;
public class DelayServiceImpl implements DelayService{
    private Integer first;
    private Integer middle;
    private Integer last;

    public DelayServiceImpl(Integer first, Integer middle, Integer last) {
        this.first = first;
        this.middle = middle;
        this.last = last;
    }

    @Override
    public void delay() {
        if(Objects.isNull(middle) || middle.equals(0)){
            try {
                Thread.sleep(1000L * randomBetween(first, last));
            } catch (InterruptedException e) {
                throw new ModelException(e.getMessage());
            }
        }
    }
}
