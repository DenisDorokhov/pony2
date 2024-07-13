package net.dorokhov.pony3.core.library.service.scan;

import net.dorokhov.pony3.api.library.domain.Album;
import net.dorokhov.pony3.api.library.domain.Artist;
import net.dorokhov.pony3.api.library.domain.Genre;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static net.dorokhov.pony3.test.ReadableAudioDataFixtures.readableAudioData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class LibraryImporterNormalizationTest extends AbstractLibraryImporterTest {

    @Test
    public void shouldNormalizeGenreName() {

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setGenre("some  value "));

        ArgumentCaptor<Genre> genreCaptor = ArgumentCaptor.forClass(Genre.class);
        verify(genreRepository).save(genreCaptor.capture());
        assertThat(genreCaptor.getValue()).satisfies(genre ->
                assertThat(genre.getName()).isEqualTo("some value"));
    }

    @Test
    public void shouldNormalizeArtistName() {

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setArtist("some  value "));

        ArgumentCaptor<Artist> artistCaptor = ArgumentCaptor.forClass(Artist.class);
        verify(artistRepository).save(artistCaptor.capture());
        assertThat(artistCaptor.getValue()).satisfies(artist ->
                assertThat(artist.getName()).isEqualTo("some value"));
    }

    @Test
    public void shouldNormalizeAlbumArtistName() {

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setAlbumArtist("some  value "));

        ArgumentCaptor<Artist> artistCaptor = ArgumentCaptor.forClass(Artist.class);
        verify(artistRepository).save(artistCaptor.capture());
        assertThat(artistCaptor.getValue()).satisfies(artist ->
                assertThat(artist.getName()).isEqualTo("some value"));
    }

    @Test
    public void shouldNormalizeAlbumName() {

        libraryImporter.importAudioData(audioNode(), readableAudioData()
                .setAlbum("some  value "));

        ArgumentCaptor<Album> albumCaptor = ArgumentCaptor.forClass(Album.class);
        verify(albumRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue()).satisfies(album ->
                assertThat(album.getName()).isEqualTo("some value"));
    }
}
