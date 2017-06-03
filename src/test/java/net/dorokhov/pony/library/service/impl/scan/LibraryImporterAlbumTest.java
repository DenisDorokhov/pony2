package net.dorokhov.pony.library.service.impl.scan;

import net.dorokhov.pony.library.domain.Album;
import net.dorokhov.pony.library.domain.Artist;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class LibraryImporterAlbumTest extends AbstractLibraryImporterTest {

    @Captor
    private ArgumentCaptor<Album> albumCaptor;
    
    @Test
    public void shouldCreateAlbum() throws Exception {
        libraryImporter.importSong(audioNode(), readableAudioDataBuilder()
                .album("someValue")
                .year(1986)
                .build());
        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue()).satisfies(album -> {
            assertThat(album.getName()).isEqualTo("someValue");
            assertThat(album.getYear()).isEqualTo(1986);
        });
    }

    @Test
    public void shouldUpdateAlbumIfNameChanged() throws Exception {
        Album existingAlbum = Album.builder()
                .artist(Artist.builder().build())
                .name("somevalue")
                .year(1986)
                .build();
        given(albumRepository.findByArtistIdAndName(any(), any())).willReturn(existingAlbum);
        libraryImporter.importSong(audioNode(), readableAudioDataBuilder()
                .album("someValue")
                .year(1986)
                .build());
        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue()).satisfies(album -> {
            assertThat(album.getName()).isEqualTo("someValue");
            assertThat(album.getYear()).isEqualTo(1986);
        });
    }

    @Test
    public void shouldUpdateAlbumIfYearChanged() throws Exception {
        Album existingAlbum = Album.builder()
                .artist(Artist.builder().build())
                .name("someValue")
                .year(1960)
                .build();
        given(albumRepository.findByArtistIdAndName(any(), any())).willReturn(existingAlbum);
        libraryImporter.importSong(audioNode(), readableAudioDataBuilder()
                .album("someValue")
                .year(1986)
                .build());
        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue()).satisfies(album -> {
            assertThat(album.getName()).isEqualTo("someValue");
            assertThat(album.getYear()).isEqualTo(1986);
        });
    }

    @Test
    public void shouldSkipAlbumIfNothingChanged() throws Exception {
        Album existingAlbum = Album.builder()
                .artist(Artist.builder().build())
                .name("someValue")
                .year(1986)
                .build();
        given(albumRepository.findByArtistIdAndName(any(), any())).willReturn(existingAlbum);
        libraryImporter.importSong(audioNode(), readableAudioDataBuilder()
                .album("someValue")
                .year(1986)
                .build());
        verify(albumRepository, never()).save((Album) any());
    }
}
