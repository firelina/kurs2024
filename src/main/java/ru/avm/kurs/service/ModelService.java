package ru.avm.kurs.service;

import ru.avm.kurs.controller.dto.PetriAgentDTO;
import ru.avm.kurs.controller.dto.StartModelDTO;
import ru.avm.kurs.stat.ModelStatistics;

import java.util.List;

public interface ModelService {
    String startModel(StartModelDTO initData);
    String stopModel(String guidModel);
    ModelStatistics getStats();
    List<PetriAgentDTO> getPetri();
}
