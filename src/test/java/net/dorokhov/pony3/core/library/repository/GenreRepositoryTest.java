package net.dorokhov.pony3.core.library.repository;

import net.dorokhov.pony3.IntegrationTest;
import net.dorokhov.pony3.api.library.domain.Genre;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GenreRepositoryTest extends IntegrationTest {

    @Autowired
    private GenreRepository genreRepository;

    @Test
    public void shouldSave() {

        Genre genre = genreRepository.save(new Genre());

        assertThat(genreRepository.findById(genre.getId())).isNotEmpty();
    }
}
