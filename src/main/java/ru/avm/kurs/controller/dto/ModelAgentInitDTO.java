package ru.avm.kurs.controller.dto;

import lombok.Data;

@Data
public class ModelAgentInitDTO {
    private String prefTitle;
    private Integer firstDelay;
    private Integer secondDelay;
    private Integer thirdDelay;
}
