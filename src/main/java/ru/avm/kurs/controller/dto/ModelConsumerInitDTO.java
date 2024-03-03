package ru.avm.kurs.controller.dto;

import lombok.Data;

@Data
public class ModelConsumerInitDTO {
    private String prefTitle;
    private Integer firstDelay;
    private Integer secondDelay;
    private Integer thirdDelay;
}
