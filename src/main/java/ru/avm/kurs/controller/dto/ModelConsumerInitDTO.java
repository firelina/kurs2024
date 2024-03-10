package ru.avm.kurs.controller.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModelConsumerInitDTO {
    private String prefTitle;
    private Integer state;
    private Integer count;
    private Integer firstDelay;
    private Integer secondDelay;
    private Integer thirdDelay;
    private Integer timeLimit;
}
