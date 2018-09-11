package net.dorokhov.pony.api.library.domain;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class GenreTest {

    @Test
    public void shouldSort() {

        Genre genre1 = Genre.builder().name("1").build();
        Genre genre2 = Genre.builder().name("2").build();
        Genre genreNull = new Genre();

        Genre[] list = {genreNull, genre2, genre1, genre2};
        Arrays.sort(list);

        assertThat(list).containsExactly(genre1, genre2, genre2, genreNull);
    }
    
    @Test
    public void shouldSupportEqualityAndHashCode() {
        
        Genre eqGenre1 = Genre.builder().id("1").build();
        Genre eqGenre2 = Genre.builder().id("1").build();
        Genre diffGenre = Genre.builder().id("2").build();

        assertThat(eqGenre1.hashCode()).isEqualTo(eqGenre2.hashCode());
        assertThat(eqGenre1.hashCode()).isNotEqualTo(diffGenre.hashCode());

        assertThat(eqGenre1).isEqualTo(eqGenre1);
        assertThat(eqGenre1).isEqualTo(eqGenre2);

        assertThat(eqGenre1).isNotEqualTo(diffGenre);
        assertThat(eqGenre1).isNotEqualTo("foo1");
        assertThat(eqGenre1).isNotEqualTo(null);
    }
}
