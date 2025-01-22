package net.dorokhov.pony2.common;

import com.google.common.base.Strings;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class SearchTermUtils {

    public static String extractSpaceSeparatedTerms(@Nullable String value) {
        return String.join(" ", extractStringTerms(value));
    }

    public static Set<String> extractStringTerms(@Nullable String value) {
        return extractTerms(value).stream()
                .flatMap(term -> {
                    List<String> result = new ArrayList<>();
                    result.add(term.value);
                    result.addAll(term.subTerms.stream()
                            .map(SubTerm::value)
                            .toList());
                    return result.stream();
                })
                .collect(Collectors.toSet());
    }

    public static List<Term> extractTerms(@Nullable String value) {
        List<Term> terms = new ArrayList<>();
        for (String word : Strings.nullToEmpty(value).split("\\p{Zs}+")) {
            List<String> subWords = new ArrayList<>(Arrays.asList(word.split("[^\\p{L}\\p{N}]+")));
            List<SubTerm> subTerms = new ArrayList<>();
            subWords.forEach(subWord -> subTerms.add(new SubTerm(subWord, SubTermType.SUB_WORD)));
            if (!subWords.isEmpty()) {
                subTerms.add(new SubTerm(String.join("", subWords), SubTermType.COMBINED_SUB_WORDS));
            }
            terms.add(new Term(word, subTerms));
        }
        return terms;
    }

    public record Term(
            String value,
            List<SubTerm> subTerms
    ) {
        public List<String> getSubWords() {
            return subTerms.stream()
                    .filter(next -> next.type == SubTermType.SUB_WORD)
                    .map(SubTerm::value)
                    .toList();
        }

        public Optional<String> getCombinedSubWords() {
            return subTerms.stream()
                    .filter(next -> next.type == SubTermType.COMBINED_SUB_WORDS)
                    .findFirst()
                    .map(SubTerm::value);
        }
    }

    public enum SubTermType {
        SUB_WORD,
        COMBINED_SUB_WORDS,
    }

    public record SubTerm(
            String value,
            SubTermType type
    ) {}
}
