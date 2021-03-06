package net.dorokhov.pony.core.library.repository;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.api.library.domain.Genre;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class GenreRepositoryTest extends IntegrationTest {

    @Autowired
    private GenreRepository genreRepository;

    @Test
    public void shouldSave() {

        Genre genre = genreRepository.save(Genre.builder().build());

        assertThat(genreRepository.findOne(genre.getId())).isNotNull();
    }
}
