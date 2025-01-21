package net.dorokhov.pony2.common;

import com.google.common.base.Strings;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchTermUtils {

    private static final Pattern SUB_WORD_PATTERN = Pattern.compile("([\\p{L}\\p{N}]+)[^\\p{L}\\p{N}]");

    public static String prepareForIndexing(@Nullable String value) {
        String normalizedValue = Strings.nullToEmpty(value);
        StringBuilder result = new StringBuilder(normalizedValue);
        for (String word : normalizedValue.split("\\s+")) {
            if (word.matches(".+[^\\p{L}\\p{N}].+")) {
                Matcher matcher = SUB_WORD_PATTERN.matcher(Strings.nullToEmpty(word) + ".");
                boolean hasMatches = false;
                List<String> subWordTerms = new ArrayList<>();
                while (matcher.find()) {
                    hasMatches = true;
                    String letterWithoutDot = matcher.group(1);
                    result.append(" ").append(letterWithoutDot);
                    subWordTerms.add(letterWithoutDot);
                }
                if (hasMatches && subWordTerms.size() > 1) {
                    result.append(" ").append(String.join("", subWordTerms.toArray(new String[]{})));
                }
            }
        }
        return result.toString();
    }
}
