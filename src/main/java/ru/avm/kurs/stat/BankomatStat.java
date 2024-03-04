package ru.avm.kurs.stat;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BankomatStat {
    private String title;
    private Integer serviced;
}
