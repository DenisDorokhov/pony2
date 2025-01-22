package net.dorokhov.pony2.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchTermUtilsTest {

    @Test
    void shouldBeNullSafe() {
        assertThat(SearchTermUtils.extractSpaceSeparatedTerms(null)).isEqualTo("");
    }

    @Test
    void shouldGenerateAcronymTerms() {
        assertThat(SearchTermUtils.extractStringTerms("W.A.S.P."))
                .containsExactlyInAnyOrder("W.A.S.P.", "W", "A", "S", "P", "WASP");
        assertThat(SearchTermUtils.extractStringTerms("W.A.S.P"))
                .containsExactlyInAnyOrder("W.A.S.P", "W", "A", "S", "P", "WASP");
        assertThat(SearchTermUtils.extractStringTerms("Back in the U.S.S.R."))
                .containsExactlyInAnyOrder("Back", "in", "the", "U.S.S.R.", "U", "S", "R", "USSR");
        assertThat(SearchTermUtils.extractStringTerms("Back in the U.S.S.R"))
                .containsExactlyInAnyOrder("Back", "in", "the", "U.S.S.R", "U", "S", "R", "USSR");
        assertThat(SearchTermUtils.extractStringTerms("Simple sentence. And another one. And"))
                .containsExactlyInAnyOrder("Simple", "sentence.", "sentence", "And", "another", "one.", "one");
        assertThat(SearchTermUtils.extractStringTerms("Simple sentence.And another one without space."))
                .containsExactlyInAnyOrder("Simple", "sentence.And", "sentence", "sentenceAnd", "And", "another", "one", "without", "space.", "space");
        assertThat(SearchTermUtils.extractStringTerms("Don't speak"))
                .containsExactlyInAnyOrder("Don't", "speak", "Don", "t", "Dont");
        assertThat(SearchTermUtils.extractStringTerms("rock-n-roll"))
                .containsExactlyInAnyOrder("rock-n-roll", "rock", "n", "roll", "rocknroll");
        assertThat(SearchTermUtils.extractStringTerms("i_am_programmer"))
                .containsExactlyInAnyOrder("i_am_programmer", "i", "am", "programmer", "iamprogrammer");
        assertThat(SearchTermUtils.extractStringTerms("The B-52's"))
                .containsExactlyInAnyOrder("The", "B-52's", "B", "52", "s", "B52s");
        assertThat(SearchTermUtils.extractStringTerms("1'33\" Until"))
                .containsExactlyInAnyOrder("1'33\"", "Until", "1", "33", "133");
        assertThat(SearchTermUtils.extractStringTerms("1'33 Until"))
                .containsExactlyInAnyOrder("1'33", "Until", "1", "33", "133");
        assertThat(SearchTermUtils.extractStringTerms("AC/DC"))
                .containsExactlyInAnyOrder("AC/DC", "AC", "DC", "ACDC");
        assertThat(SearchTermUtils.extractStringTerms("артист, другой . entity2"))
                .containsExactlyInAnyOrder("артист,", "артист", "другой", ".", "entity2");
    }
}
