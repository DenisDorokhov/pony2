package net.dorokhov.pony2.common;

import com.google.common.base.Strings;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchTermUtils {

    private static final Pattern PATTERN_ACRONYM = Pattern.compile("(\\p{L}+)[.'-_]");
    private static final Set<String> ACRONYM_SEPARATORS = Set.of(".", "'", "-", "_");

    public static String prepareForIndexing(@Nullable String value) {
        String normalizedValue = Strings.nullToEmpty(value);
        StringBuilder result = new StringBuilder(normalizedValue);
        for (String word : normalizedValue.split("\\s+")) {
            if (ACRONYM_SEPARATORS.stream().anyMatch(word::contains)) {
                Matcher matcher = PATTERN_ACRONYM.matcher(Strings.nullToEmpty(word + "."));
                boolean hasMatches = false;
                List<String> acronymTerms = new ArrayList<>();
                while (matcher.find()) {
                    hasMatches = true;
                    String letterWithoutDot = matcher.group(1);
                    result.append(" ").append(letterWithoutDot);
                    acronymTerms.add(letterWithoutDot);
                }
                if (hasMatches && acronymTerms.size() > 1) {
                    result.append(" ").append(String.join("", acronymTerms.toArray(new String[]{})));
                }
            }
        }
        return result.toString();
    }
}
