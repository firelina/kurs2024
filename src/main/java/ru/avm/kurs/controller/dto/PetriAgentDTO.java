package ru.avm.kurs.controller.dto;

import lombok.Data;

import java.util.ArrayList;

@Data
public class PetriAgentDTO {
    private String title;
    private ArrayList<Integer> states = new ArrayList<>();

    public PetriAgentDTO(String title) {
        this.title = title;
    }
}
