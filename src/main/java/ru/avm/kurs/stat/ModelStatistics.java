package ru.avm.kurs.stat;

import lombok.Data;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ModelStatistics {
    private ConcurrentHashMap<String, SourceStat> bankomatMap;
    private ConcurrentHashMap<String, SourceStat> clerkMap;
    private List<SourceStat> bankomats;
    private List<SourceStat> clerks;
    private Integer sizeBankomatQueue = 0;
    private Integer sizeClerktQueue = 0;
    private Integer bankomatNotServed = 0;
    private Integer clerkNotServed = 0;

    public ModelStatistics() {
        bankomatMap = new ConcurrentHashMap<>();
        clerkMap = new ConcurrentHashMap<>();
    }
}
