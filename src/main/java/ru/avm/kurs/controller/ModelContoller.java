package ru.avm.kurs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.avm.kurs.controller.dto.StartModelDTO;
import ru.avm.kurs.service.ModelService;
import ru.avm.kurs.stat.ModelStatistics;

@RestController
@RequiredArgsConstructor
public class ModelContoller {
    private final ModelService modelService;
    @PostMapping("/start")
    public ResponseEntity<String> startModel(@RequestBody StartModelDTO startDTO){
        return ResponseEntity.ok(modelService.startModel(startDTO));
    }
    @GetMapping("/stop/{guid}")
    public ResponseEntity<String> stopModel(@PathVariable(name = "guid") String guidModel){
        return ResponseEntity.ok(modelService.stopModel(guidModel));
    }
    @GetMapping("/test")
    public ResponseEntity<String> testModel(){
        return ResponseEntity.ok(modelService.startModel(null));
    }
    @GetMapping("/stat")
    public ResponseEntity<ModelStatistics> stats(){
        return ResponseEntity.ok(modelService.getStats());
    }

}
