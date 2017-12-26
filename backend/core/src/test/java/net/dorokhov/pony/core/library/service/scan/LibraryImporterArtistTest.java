package net.dorokhov.pony.core.library.service.scan;

import net.dorokhov.pony.api.library.domain.Artist;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import static net.dorokhov.pony.test.ReadableAudioDataFixtures.readableAudioDataBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LibraryImporterArtistTest extends AbstractLibraryImporterTest {

    @Captor
    private ArgumentCaptor<Artist> artistCaptor;

    @Test
    public void shouldCreateArtist() {

        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .artist("someValue")
                .build());

        verify(artistRepository).save(artistCaptor.capture());
        assertThat(artistCaptor.getValue()).satisfies(artist ->
                assertThat(artist.getName()).isEqualTo("someValue"));
    }

    @Test
    public void shouldUpdateArtistIfNameChanged() {

        Artist existingArtist = Artist.builder().name("somevalue").build();
        when(artistRepository.findByName(any())).thenReturn(existingArtist);

        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .artist("someValue")
                .build());

        verify(artistRepository).save(artistCaptor.capture());
        assertThat(artistCaptor.getValue()).satisfies(artist ->
                assertThat(artist.getName()).isEqualTo("someValue"));
    }

    @Test
    public void shouldUpdateArtistIfAlbumArtistChanged() {

        Artist existingArtist = Artist.builder().name("somevalue").build();
        when(artistRepository.findByName(any())).thenReturn(existingArtist);

        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .albumArtist("someValue")
                .build());

        verify(artistRepository).save(artistCaptor.capture());
        assertThat(artistCaptor.getValue()).satisfies(artist ->
                assertThat(artist.getName()).isEqualTo("someValue"));
    }

    @Test
    public void shouldSkipArtistIfNothingChanged() {

        Artist existingArtist = Artist.builder().name("someValue").build();
        when(artistRepository.findByName(any())).thenReturn(existingArtist);

        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .artist("someValue")
                .build());

        verify(artistRepository, never()).save((Artist) any());
    }
}
