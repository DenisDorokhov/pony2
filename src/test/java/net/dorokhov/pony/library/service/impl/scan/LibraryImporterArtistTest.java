package net.dorokhov.pony.library.service.impl.scan;

import net.dorokhov.pony.library.domain.Artist;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class LibraryImporterArtistTest extends AbstractLibraryImporterTest {

    @Captor
    private ArgumentCaptor<Artist> artistCaptor;

    @Test
    public void shouldCreateArtist() throws Exception {
        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .artist("someValue")
                .build());
        verify(artistRepository).save(artistCaptor.capture());
        assertThat(artistCaptor.getValue()).satisfies(artist ->
                assertThat(artist.getName()).isEqualTo("someValue"));
    }

    @Test
    public void shouldUpdateArtistIfNameChanged() throws Exception {
        Artist existingArtist = Artist.builder().name("somevalue").build();
        given(artistRepository.findByName(any())).willReturn(existingArtist);
        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .artist("someValue")
                .build());
        verify(artistRepository).save(artistCaptor.capture());
        assertThat(artistCaptor.getValue()).satisfies(artist ->
                assertThat(artist.getName()).isEqualTo("someValue"));
    }

    @Test
    public void shouldUpdateArtistIfAlbumArtistChanged() throws Exception {
        Artist existingArtist = Artist.builder().name("somevalue").build();
        given(artistRepository.findByName(any())).willReturn(existingArtist);
        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .albumArtist("someValue")
                .build());
        verify(artistRepository).save(artistCaptor.capture());
        assertThat(artistCaptor.getValue()).satisfies(artist ->
                assertThat(artist.getName()).isEqualTo("someValue"));
    }

    @Test
    public void shouldSkipArtistIfNothingChanged() throws Exception {
        Artist existingArtist = Artist.builder().name("someValue").build();
        given(artistRepository.findByName(any())).willReturn(existingArtist);
        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .artist("someValue")
                .build());
        verify(artistRepository, never()).save((Artist) any());
    }
}
