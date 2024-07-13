package net.dorokhov.pony3.core.library.service.scan;

import net.dorokhov.pony3.api.library.domain.Artist;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import static net.dorokhov.pony3.test.ReadableAudioDataFixtures.readableAudioData;
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
        assertThat(artistCaptor.getValue()).satisfies(artist ->
                assertThat(artist.getName()).isEqualTo("someValue"));
    }

    @Test
    public void shouldUpdateArtistIfNameChanged() {

        Artist existingArtist = new Artist().setName("somevalue");
        when(artistRepository.findByName(any())).thenReturn(existingArtist);

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setArtist("someValue"));

        verify(artistRepository).save(artistCaptor.capture());
        assertThat(artistCaptor.getValue()).satisfies(artist ->
                assertThat(artist.getName()).isEqualTo("someValue"));
    }

    @Test
    public void shouldUpdateArtistIfAlbumArtistChanged() {

        Artist existingArtist = new Artist().setName("somevalue");
        when(artistRepository.findByName(any())).thenReturn(existingArtist);

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setAlbumArtist("someValue"));

        verify(artistRepository).save(artistCaptor.capture());
        assertThat(artistCaptor.getValue()).satisfies(artist ->
                assertThat(artist.getName()).isEqualTo("someValue"));
    }

    @Test
    public void shouldSkipArtistIfNothingChanged() {

        Artist existingArtist = new Artist().setName("someValue");
        when(artistRepository.findByName(any())).thenReturn(existingArtist);

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setArtist("someValue"));

        verify(artistRepository, never()).save(any());
    }
}
