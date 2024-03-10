package ru.avm.kurs.util;

import java.util.Random;

public class ModelUtil {
    public static Integer randomBetween(int minimum, int maximum){
        Random rn = new Random();
        return rn.nextInt(maximum - minimum + 1) + minimum;
    }
}
