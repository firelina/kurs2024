package ru.avm.kurs.stat;

import lombok.Data;
import ru.avm.kurs.stat.BankomatStat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ModelStatistics {
    private List<Integer> generatedActors = new ArrayList<>();
    private Map<String, Integer> bankomatMap = new HashMap<>();
    private Map<String, Integer> clerkMap = new HashMap<>();;
    private List<BankomatStat> bankomats;
    private List<ClerkStat> clerks;
}
