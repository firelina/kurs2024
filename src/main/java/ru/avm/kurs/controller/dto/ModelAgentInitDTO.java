package ru.avm.kurs.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ModelAgentInitDTO {
    private String prefTitle;
    private Integer firstDelay;
    private Integer secondDelay;
    private Integer thirdDelay;
}
