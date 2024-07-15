package net.dorokhov.pony3.core.library.service.scan;

import net.dorokhov.pony3.api.library.domain.Genre;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import static net.dorokhov.pony3.test.ReadableAudioDataFixtures.readableAudioData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LibraryImporterGenreTest extends AbstractLibraryImporterTest {
    
    @Captor
    private ArgumentCaptor<Genre> genreCaptor;

    @Test
    public void shouldCreateGenre() {

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setGenre("someValue"));

        verify(genreRepository).save(genreCaptor.capture());
        assertThat(genreCaptor.getValue()).satisfies(genre ->
                assertThat(genre.getName()).isEqualTo("someValue"));
    }

    @Test
    public void shouldUpdateGenreIfNameChanged() {

        Genre existingGenre = new Genre().setName("somevalue");
        when(genreRepository.findByName(any())).thenReturn(existingGenre);

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setGenre("someValue"));

        verify(genreRepository).save(genreCaptor.capture());
        assertThat(genreCaptor.getValue()).satisfies(genre ->
                assertThat(genre.getName()).isEqualTo("someValue"));
    }

    @Test
    public void shouldSkipGenreIfNothingChanged() {

        Genre existingGenre = new Genre().setName("someValue");
        when(genreRepository.findByName(any())).thenReturn(existingGenre);

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setGenre("someValue"));

        verify(genreRepository, never()).save(any());
    }
}
