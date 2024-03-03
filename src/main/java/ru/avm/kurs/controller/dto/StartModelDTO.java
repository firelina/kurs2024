package ru.avm.kurs.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class StartModelDTO {
    private ModelActorInitDTO actorInitDTO;
    private List<ModelConsumerInitDTO> consumerInitDTOList;
}
