package net.dorokhov.pony.core.library.service.scan;

import net.dorokhov.pony.api.library.domain.Album;
import net.dorokhov.pony.api.library.domain.Artist;
import net.dorokhov.pony.api.library.domain.Genre;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static net.dorokhov.pony.test.ReadableAudioDataFixtures.readableAudioDataBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class LibraryImporterNormalizationTest extends AbstractLibraryImporterTest {

    @Test
    public void shouldNormalizeGenreName() throws Exception {
        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .genre("some  value ")
                .build());
        ArgumentCaptor<Genre> genreCaptor = ArgumentCaptor.forClass(Genre.class);
        verify(genreRepository).save(genreCaptor.capture());
        assertThat(genreCaptor.getValue()).satisfies(genre ->
                assertThat(genre.getName()).isEqualTo("some value"));
    }

    @Test
    public void shouldNormalizeArtistName() throws Exception {
        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .artist("some  value ")
                .build());
        ArgumentCaptor<Artist> artistCaptor = ArgumentCaptor.forClass(Artist.class);
        verify(artistRepository).save(artistCaptor.capture());
        assertThat(artistCaptor.getValue()).satisfies(artist ->
                assertThat(artist.getName()).isEqualTo("some value"));
    }

    @Test
    public void shouldNormalizeAlbumArtistName() throws Exception {
        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .albumArtist("some  value ")
                .build());
        ArgumentCaptor<Artist> artistCaptor = ArgumentCaptor.forClass(Artist.class);
        verify(artistRepository).save(artistCaptor.capture());
        assertThat(artistCaptor.getValue()).satisfies(artist ->
                assertThat(artist.getName()).isEqualTo("some value"));
    }

    @Test
    public void shouldNormalizeAlbumName() throws Exception {
        libraryImporter.importAudioData(audioNode(), readableAudioDataBuilder()
                .album("some  value ")
                .build());
        ArgumentCaptor<Album> albumCaptor = ArgumentCaptor.forClass(Album.class);
        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue()).satisfies(album ->
                assertThat(album.getName()).isEqualTo("some value"));
    }
}
