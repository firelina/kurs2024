package ru.avm.kurs.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class StartModelDTO {
    private ModelAgentInitDTO agent;
    private List<ModelConsumerInitDTO> consumers;
}
