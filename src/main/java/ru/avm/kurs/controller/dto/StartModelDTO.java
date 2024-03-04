package ru.avm.kurs.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class StartModelDTO {
    private ModelAgentInitDTO modelAgentInitDTO;
    private List<ModelConsumerInitDTO> consumenrs;
}
