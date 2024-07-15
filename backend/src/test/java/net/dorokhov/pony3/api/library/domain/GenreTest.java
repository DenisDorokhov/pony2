package net.dorokhov.pony3.api.library.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class GenreTest {

    @Test
    public void shouldSort() {

        Genre genre1 = new Genre()
                .setId("1")
                .setName("1");
        Genre genre2 = new Genre()
                .setId("2")
                .setName("2");
        Genre genreNull = new Genre()
                .setId("null");

        Genre[] list = {genreNull, genre2, genre1, genre2};
        Arrays.sort(list);

        assertThat(list).containsExactly(genre1, genre2, genre2, genreNull);
    }
    
    @Test
    public void shouldSupportEqualityAndHashCode() {
        
        Genre eqGenre1 = new Genre().setId("1");
        Genre eqGenre2 = new Genre().setId("1");
        Genre diffGenre = new Genre().setId("2");

        assertThat(eqGenre1.hashCode()).isEqualTo(eqGenre2.hashCode());
        assertThat(eqGenre1.hashCode()).isNotEqualTo(diffGenre.hashCode());

        assertThat(eqGenre1).isEqualTo(eqGenre1);
        assertThat(eqGenre1).isEqualTo(eqGenre2);

        assertThat(eqGenre1).isNotEqualTo(diffGenre);
        assertThat(eqGenre1).isNotEqualTo("foo1");
        assertThat(eqGenre1).isNotEqualTo(null);
    }
}
