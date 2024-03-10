package ru.avm.kurs.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ModelAgent {
    private String title;
    private Integer state;
    private Long startTime = System.currentTimeMillis();

    public ModelAgent(String title, Integer state) {
        this.title = title;
        this.state = state;
    }
}
