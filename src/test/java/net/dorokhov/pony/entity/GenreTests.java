package net.dorokhov.pony.entity;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class GenreTests {

    @Test
    public void sort() throws Exception {

        Genre genre1 = new Genre();
        genre1.setName("1");
        Genre genre2 = new Genre();
        genre2.setName("2");
        Genre genreNull = new Genre();
        genreNull.setName(null);

        Genre[] list = {genreNull, genre2, genre1, genre2};
        Arrays.sort(list);

        assertThat(list).containsExactly(genre1, genre2, genre2, genreNull);
    }
    @Test
    public void supportEqualityAndHashCode() throws Exception {
        
        Genre eqGenre1 = new Genre();
        eqGenre1.setId(1L);
        Genre eqGenre2 = new Genre();
        eqGenre2.setId(1L);
        Genre diffGenre = new Genre();
        diffGenre.setId(2L);

        assertThat(eqGenre1.hashCode()).isEqualTo(eqGenre2.hashCode());
        assertThat(eqGenre1.hashCode()).isNotEqualTo(diffGenre.hashCode());

        assertThat(eqGenre1).isEqualTo(eqGenre1);
        assertThat(eqGenre1).isEqualTo(eqGenre2);

        assertThat(eqGenre1).isNotEqualTo(diffGenre);
        assertThat(eqGenre1).isNotEqualTo("foo1");
        assertThat(eqGenre1).isNotEqualTo(null);
    }
}
