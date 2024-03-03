package ru.avm.kurs.service;

import ru.avm.kurs.controller.dto.StartModelDTO;

public interface ModelService {
    String startModel(StartModelDTO initData);
    String stopModel(String guidModel);
}
