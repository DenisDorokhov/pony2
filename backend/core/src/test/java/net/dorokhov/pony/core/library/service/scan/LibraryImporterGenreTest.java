package net.dorokhov.pony.core.library.service.scan;

import net.dorokhov.pony.api.library.domain.Genre;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import static net.dorokhov.pony.test.ReadableAudioDataFixtures.readableAudioDataBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LibraryImporterGenreTest extends AbstractLibraryImporterTest {
    
    @Captor
    private ArgumentCaptor<Genre> genreCaptor;

    @Test
    public void shouldCreateGenre() {

        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .genre("someValue")
                .build());

        verify(genreRepository).save(genreCaptor.capture());
        assertThat(genreCaptor.getValue()).satisfies(genre ->
                assertThat(genre.getName()).isEqualTo("someValue"));
    }

    @Test
    public void shouldUpdateGenreIfNameChanged() {

        Genre existingGenre = Genre.builder().name("somevalue").build();
        when(genreRepository.findByName(any())).thenReturn(existingGenre);

        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .genre("someValue")
                .build());

        verify(genreRepository).save(genreCaptor.capture());
        assertThat(genreCaptor.getValue()).satisfies(genre ->
                assertThat(genre.getName()).isEqualTo("someValue"));
    }

    @Test
    public void shouldSkipGenreIfNothingChanged() {

        Genre existingGenre = Genre.builder().name("someValue").build();
        when(genreRepository.findByName(any())).thenReturn(existingGenre);

        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .genre("someValue")
                .build());

        verify(genreRepository, never()).save((Genre) any());
    }
}
