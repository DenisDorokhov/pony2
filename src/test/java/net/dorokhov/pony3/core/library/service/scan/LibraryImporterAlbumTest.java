package net.dorokhov.pony3.core.library.service.scan;

import net.dorokhov.pony3.api.library.domain.Album;
import net.dorokhov.pony3.api.library.domain.Artist;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import static net.dorokhov.pony3.test.ReadableAudioDataFixtures.readableAudioData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LibraryImporterAlbumTest extends AbstractLibraryImporterTest {

    @Captor
    private ArgumentCaptor<Album> albumCaptor;
    
    @Test
    public void shouldCreateAlbum() {

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setAlbum("someValue")
                .setYear(1986));

        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue()).satisfies(album -> {
            assertThat(album.getName()).isEqualTo("someValue");
            assertThat(album.getYear()).isEqualTo(1986);
        });
    }

    @Test
    public void shouldUpdateAlbumIfNameChanged() {

        Album existingAlbum = new Album()
                .setArtist(new Artist())
                .setName("somevalue")
                .setYear(1986);
        when(albumRepository.findByArtistIdAndName(any(), any())).thenReturn(existingAlbum);

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setAlbum("someValue")
                .setYear(1986));

        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue()).satisfies(album -> {
            assertThat(album.getName()).isEqualTo("someValue");
            assertThat(album.getYear()).isEqualTo(1986);
        });
    }

    @Test
    public void shouldUpdateAlbumIfYearChanged() {

        Album existingAlbum = new Album()
                .setArtist(new Artist())
                .setName("someValue")
                .setYear(1960);
        when(albumRepository.findByArtistIdAndName(any(), any())).thenReturn(existingAlbum);

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setAlbum("someValue")
                .setYear(1986));

        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue()).satisfies(album -> {
            assertThat(album.getName()).isEqualTo("someValue");
            assertThat(album.getYear()).isEqualTo(1986);
        });
    }

    @Test
    public void shouldSkipAlbumIfNothingChanged() {

        Album existingAlbum = new Album()
                .setArtist(new Artist())
                .setName("someValue")
                .setYear(1986);
        when(albumRepository.findByArtistIdAndName(any(), any())).thenReturn(existingAlbum);

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setAlbum("someValue")
                .setYear(1986));

        verify(albumRepository, never()).save(any());
    }
}
