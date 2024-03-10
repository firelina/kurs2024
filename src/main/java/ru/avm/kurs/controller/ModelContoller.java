package ru.avm.kurs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.avm.kurs.controller.dto.ResultDTO;
import ru.avm.kurs.controller.dto.StartModelDTO;
import ru.avm.kurs.service.ModelService;
import ru.avm.kurs.stat.ModelStatistics;
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequiredArgsConstructor
public class ModelContoller {
    private final ModelService modelService;
    @PostMapping("/start")
    public ResponseEntity<ResultDTO> startModel(@RequestBody StartModelDTO startDTO){
        return ResponseEntity.ok(new ResultDTO(modelService.startModel(startDTO)));
    }
    @GetMapping("/stop/{guid}")
    public ResponseEntity<ResultDTO> stopModel(@PathVariable(name = "guid") String guidModel){
        return ResponseEntity.ok(new ResultDTO(modelService.stopModel(guidModel)));
    }
    @GetMapping("/test")
    public ResponseEntity<ResultDTO> testModel(){
        return ResponseEntity.ok(new ResultDTO(modelService.startModel(null)));
    }
    @GetMapping("/stat")
    public ResponseEntity<ModelStatistics> stats(){
        return ResponseEntity.ok(modelService.getStats());
    }

}
