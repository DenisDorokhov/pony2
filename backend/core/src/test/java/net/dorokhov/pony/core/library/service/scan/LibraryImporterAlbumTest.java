package net.dorokhov.pony.core.library.service.scan;

import net.dorokhov.pony.api.library.domain.Album;
import net.dorokhov.pony.api.library.domain.Artist;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import static net.dorokhov.pony.test.ReadableAudioDataFixtures.readableAudioDataBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LibraryImporterAlbumTest extends AbstractLibraryImporterTest {

    @Captor
    private ArgumentCaptor<Album> albumCaptor;
    
    @Test
    public void shouldCreateAlbum() {

        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
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
    public void shouldUpdateAlbumIfNameChanged() {

        Album existingAlbum = Album.builder()
                .artist(Artist.builder().build())
                .name("somevalue")
                .year(1986)
                .build();
        when(albumRepository.findByArtistIdAndName(any(), any())).thenReturn(existingAlbum);

        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
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
    public void shouldUpdateAlbumIfYearChanged() {

        Album existingAlbum = Album.builder()
                .artist(Artist.builder().build())
                .name("someValue")
                .year(1960)
                .build();
        when(albumRepository.findByArtistIdAndName(any(), any())).thenReturn(existingAlbum);

        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
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
    public void shouldSkipAlbumIfNothingChanged() {

        Album existingAlbum = Album.builder()
                .artist(Artist.builder().build())
                .name("someValue")
                .year(1986)
                .build();
        when(albumRepository.findByArtistIdAndName(any(), any())).thenReturn(existingAlbum);

        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .album("someValue")
                .year(1986)
                .build());

        verify(albumRepository, never()).save((Album) any());
    }
}
