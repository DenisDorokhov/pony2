package net.dorokhov.pony2.core.library.service.scan;

import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistGenre;
import net.dorokhov.pony2.api.library.domain.Genre;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.util.List;

import static net.dorokhov.pony2.test.ReadableAudioDataFixtures.readableAudioData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LibraryImporterArtistTest extends AbstractLibraryImporterTest {

    @Captor
    private ArgumentCaptor<Artist> artistCaptor;

    @Test
    public void shouldCreateArtist() {

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setArtist("someValue"));

        verify(artistRepository).save(artistCaptor.capture());
        assertThat(artistCaptor.getValue()).satisfies(artist -> {
            assertThat(artist.getName()).isEqualTo("someValue");
            assertThat(artist.getGenres()).hasSize(1);
            assertThat(artist.getGenres().getFirst()).satisfies(artistGenre -> {
                assertThat(artistGenre.getArtist()).isNotNull();
                assertThat(artistGenre.getGenre()).isNotNull();
            });
        });
    }

    @Test
    public void shouldUpdateArtistIfNameChanged() {

        Artist existingArtist = new Artist().setName("somevalue");
        when(artistRepository.findByName(any())).thenReturn(existingArtist);

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setArtist("someValue"));

        verify(artistRepository).save(artistCaptor.capture());
        assertThat(artistCaptor.getValue()).satisfies(artist -> {
            assertThat(artist.getName()).isEqualTo("someValue");
            assertThat(artist.getGenres()).hasSize(1);
            assertThat(artist.getGenres().getFirst()).satisfies(artistGenre -> {
                assertThat(artistGenre.getArtist()).isNotNull();
                assertThat(artistGenre.getGenre()).isNotNull();
            });
        });
    }

    @Test
    public void shouldUpdateArtistIfAlbumArtistChanged() {

        Artist existingArtist = new Artist().setName("somevalue");
        when(artistRepository.findByName(any())).thenReturn(existingArtist);

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setAlbumArtist("someValue"));

        verify(artistRepository).save(artistCaptor.capture());
        assertThat(artistCaptor.getValue()).satisfies(artist -> {
            assertThat(artist.getName()).isEqualTo("someValue");
            assertThat(artist.getGenres()).hasSize(1);
            assertThat(artist.getGenres().getFirst()).satisfies(artistGenre -> {
                assertThat(artistGenre.getArtist()).isNotNull();
                assertThat(artistGenre.getGenre()).isNotNull();
            });
        });
    }

    @Test
    public void shouldSkipArtistIfNothingChanged() {

        Genre existingGenre = new Genre();
        when(genreRepository.findByName(any())).thenReturn(existingGenre);

        Artist existingArtist = new Artist().setName("someValue");
        existingArtist.setGenres(List.of(new ArtistGenre().setArtist(existingArtist).setGenre(existingGenre)));
        when(artistRepository.findByName(any())).thenReturn(existingArtist);

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setArtist("someValue"));

        verify(artistRepository, never()).save(any());
    }
}
