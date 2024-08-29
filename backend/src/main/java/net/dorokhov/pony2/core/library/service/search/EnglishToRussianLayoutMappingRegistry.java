package net.dorokhov.pony2.core.library.service.search;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public final class EnglishToRussianLayoutMappingRegistry {

    private static final Map<String, String> MAPPING = ImmutableMap.<String, String>builder()
            .put("F", "А")
            .put("<", "Б")
            .put("D", "В")
            .put("U", "Г")
            .put("L", "Д")
            .put("T", "Е")
            .put("~", "Ё")
            .put(":", "Ж")
            .put("P", "З")
            .put("B", "И")
            .put("Q", "Й")
            .put("R", "К")
            .put("K", "Л")
            .put("V", "М")
            .put("Y", "Н")
            .put("J", "О")
            .put("G", "П")
            .put("H", "Р")
            .put("C", "С")
            .put("N", "Т")
            .put("E", "У")
            .put("A", "Ф")
            .put("{", "Х")
            .put("W", "Ц")
            .put("X", "Ч")
            .put("I", "Ш")
            .put("O", "Щ")
            .put("S", "Ы")
            .put("\"", "Э")
            .put(">", "Ю")
            .put("Z", "Я")
            .put("f", "а")
            .put(",", "б")
            .put("d", "в")
            .put("u", "г")
            .put("l", "д")
            .put("t", "е")
            .put("`", "ё")
            .put(";", "ж")
            .put("p", "з")
            .put("b", "и")
            .put("q", "й")
            .put("r", "к")
            .put("k", "л")
            .put("v", "м")
            .put("y", "н")
            .put("j", "о")
            .put("g", "п")
            .put("h", "р")
            .put("c", "с")
            .put("n", "т")
            .put("e", "у")
            .put("a", "ф")
            .put("[", "х")
            .put("w", "ц")
            .put("x", "ч")
            .put("i", "ш")
            .put("o", "щ")
            .put("s", "ы")
            .put("'", "э")
            .put(".", "ю")
            .put("z", "я")
            .put("}", "Ъ")
            .put("]", "ъ")
            .put("M", "Ь")
            .put("m", "ь")
            .build();

    public static Map<String, String> mapping() {
        return MAPPING;
    }
}
