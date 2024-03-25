package ru.avm.kurs.stat;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SourceStat {
    private String title;
    private Integer serviced;
    private Boolean isBusy;
}
