package ru.avm.kurs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.avm.kurs.controller.dto.StartModelDTO;
import ru.avm.kurs.service.ModelService;

@RestController
@RequiredArgsConstructor
public class ModelContoller {
    private final ModelService modelService;
    @PostMapping("/start")
    public ResponseEntity<String> startModel(@RequestBody StartModelDTO startDTO){
        return ResponseEntity.ok(modelService.startModel(startDTO));
    }
    @PostMapping("/stop")
    public ResponseEntity<String> stopModel(@RequestBody String guidModel){
        return ResponseEntity.ok(modelService.stopModel(guidModel));
    }
    @GetMapping("/test")
    public ResponseEntity<String> testModel(){
        return ResponseEntity.ok(modelService.startModel(null));
    }
}
