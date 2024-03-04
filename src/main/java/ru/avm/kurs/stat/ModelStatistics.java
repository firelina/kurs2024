package ru.avm.kurs.stat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import ru.avm.kurs.stat.BankomatStat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ModelStatistics {
    private ConcurrentHashMap<String, BankomatStat> bankomatMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ClerkStat> clerkMap = new ConcurrentHashMap<>();
    private List<BankomatStat> bankomats;
    private List<ClerkStat> clerks;
    private Integer sizeBankomatQueue;
    private Integer sizeClerktQueue;
}
