package ru.avm.kurs.service;

import ru.avm.kurs.controller.dto.StartModelDTO;
import ru.avm.kurs.stat.ModelStatistics;

public interface ModelService {
    String startModel(StartModelDTO initData);
    String stopModel(String guidModel);
    ModelStatistics getStats();
}
