package net.dorokhov.pony2.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SearchTermUtilsTest {

    @Test
    void shouldBeNullSafe() {
        assertThat(SearchTermUtils.prepareForIndexing(null)).isEqualTo("");
    }

    @Test
    void shouldGenerateAcronymTerms() {
        assertThat(SearchTermUtils.prepareForIndexing("W.A.S.P."))
                .isEqualTo("W.A.S.P. W A S P WASP");
        assertThat(SearchTermUtils.prepareForIndexing("W.A.S.P"))
                .isEqualTo("W.A.S.P W A S P WASP");
        assertThat(SearchTermUtils.prepareForIndexing("Back in the U.S.S.R."))
                .isEqualTo("Back in the U.S.S.R. U S S R USSR");
        assertThat(SearchTermUtils.prepareForIndexing("Back in the U.S.S.R"))
                .isEqualTo("Back in the U.S.S.R U S S R USSR");
        assertThat(SearchTermUtils.prepareForIndexing("Simple sentence. And another one. And"))
                .isEqualTo("Simple sentence. And another one. And sentence one");
        assertThat(SearchTermUtils.prepareForIndexing("Simple sentence.And another one without space."))
                .isEqualTo("Simple sentence.And another one without space. sentence And sentenceAnd space");
        assertThat(SearchTermUtils.prepareForIndexing("Don't speak"))
                .isEqualTo("Don't speak Don t Dont");
        assertThat(SearchTermUtils.prepareForIndexing("rock-n-roll"))
                .isEqualTo("rock-n-roll rock n roll rocknroll");
        assertThat(SearchTermUtils.prepareForIndexing("i_am_programmer"))
                .isEqualTo("i_am_programmer i am programmer iamprogrammer");
    }
}