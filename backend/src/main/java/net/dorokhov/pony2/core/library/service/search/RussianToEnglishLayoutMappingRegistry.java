package net.dorokhov.pony2.core.library.service.search;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public final class RussianToEnglishLayoutMappingRegistry {

    private static final Map<String, String> MAPPING = ImmutableMap.<String, String>builder()
            .put("Ф", "A")
            .put("И", "B")
            .put("С", "C")
            .put("В", "D")
            .put("У", "E")
            .put("А", "F")
            .put("П", "G")
            .put("Р", "H")
            .put("Ш", "I")
            .put("О", "J")
            .put("Л", "K")
            .put("Д", "L")
            .put("Ь", "M")
            .put("Т", "N")
            .put("Щ", "O")
            .put("З", "P")
            .put("Й", "Q")
            .put("К", "R")
            .put("Ы", "S")
            .put("Е", "T")
            .put("Г", "U")
            .put("М", "V")
            .put("Ц", "W")
            .put("Ч", "X")
            .put("Н", "Y")
            .put("Я", "Z")
            .put("ф", "a")
            .put("и", "b")
            .put("с", "c")
            .put("в", "d")
            .put("у", "e")
            .put("а", "f")
            .put("п", "g")
            .put("р", "h")
            .put("ш", "i")
            .put("о", "j")
            .put("л", "k")
            .put("д", "l")
            .put("ь", "m")
            .put("т", "n")
            .put("щ", "o")
            .put("з", "p")
            .put("й", "q")
            .put("к", "r")
            .put("ы", "s")
            .put("е", "t")
            .put("г", "u")
            .put("м", "v")
            .put("ц", "w")
            .put("ч", "x")
            .put("н", "y")
            .put("я", "z")
            .build();

    public static Map<String, String> mapping() {
        return MAPPING;
    }
}
