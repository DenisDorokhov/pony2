package net.dorokhov.pony2.core.library.service.search;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Additional ASCII folding that is not implemented in Lucene.
 */
public final class TransliterationMappingRegistry {

    private static final Map<String, String> MAPPING = ImmutableMap.<String, String>builder()
            .put("А", "A")
            .put("Б", "B")
            .put("В", "V")
            .put("Г", "G")
            .put("Д", "D")
            .put("Е", "E")
            .put("Ё", "E")
            .put("Ж", "ZH")
            .put("З", "Z")
            .put("И", "I")
            .put("Й", "I")
            .put("К", "K")
            .put("Л", "L")
            .put("М", "M")
            .put("Н", "N")
            .put("О", "O")
            .put("П", "P")
            .put("Р", "R")
            .put("С", "S")
            .put("Т", "T")
            .put("У", "U")
            .put("Ф", "F")
            .put("Х", "KH")
            .put("Ц", "TS")
            .put("Ч", "CH")
            .put("Ш", "SH")
            .put("Щ", "SHCH")
            .put("Ы", "Y")
            .put("Э", "E")
            .put("Ю", "YU")
            .put("Я", "YA")
            .put("а", "A")
            .put("б", "B")
            .put("в", "V")
            .put("г", "G")
            .put("д", "D")
            .put("е", "E")
            .put("ё", "E")
            .put("ж", "ZH")
            .put("з", "Z")
            .put("и", "I")
            .put("й", "I")
            .put("к", "K")
            .put("л", "L")
            .put("м", "M")
            .put("н", "N")
            .put("о", "O")
            .put("п", "P")
            .put("р", "R")
            .put("с", "S")
            .put("т", "T")
            .put("у", "U")
            .put("ф", "F")
            .put("х", "KH")
            .put("ц", "TS")
            .put("ч", "CH")
            .put("ш", "SH")
            .put("щ", "SHCH")
            .put("ы", "Y")
            .put("э", "E")
            .put("ю", "YU")
            .put("я", "YA")
            .put("Ъ", "")
            .put("ъ", "")
            .put("Ь", "")
            .put("ь", "")
            .build();

    public static Map<String, String> mapping() {
        return MAPPING;
    }
}
