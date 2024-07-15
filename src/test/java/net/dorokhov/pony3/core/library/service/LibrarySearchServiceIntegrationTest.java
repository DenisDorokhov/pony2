package net.dorokhov.pony3.core.library.service;

import net.dorokhov.pony3.IntegrationTest;
import net.dorokhov.pony3.api.library.domain.Album;
import net.dorokhov.pony3.api.library.domain.Artist;
import net.dorokhov.pony3.api.library.domain.FileType;
import net.dorokhov.pony3.api.library.domain.Genre;
import net.dorokhov.pony3.api.library.domain.LibrarySearchQuery;
import net.dorokhov.pony3.api.library.domain.Song;
import net.dorokhov.pony3.api.library.service.LibrarySearchService;
import net.dorokhov.pony3.core.library.repository.AlbumRepository;
import net.dorokhov.pony3.core.library.repository.ArtistRepository;
import net.dorokhov.pony3.core.library.repository.GenreRepository;
import net.dorokhov.pony3.core.library.repository.SongRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class LibrarySearchServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private LibrarySearchService librarySearchService;

    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private SongRepository songRepository;

    @Test
    public void shouldSearchGenres() {

        Genre genre1 = genreRepository.save(buildGenre("the foobar entity1"));
        Genre genre2 = genreRepository.save(buildGenre("the foobar entity2"));

        assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("foo"), 10)).containsOnly(genre1, genre2);
        assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
        assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("ent th foo"), 10)).containsOnly(genre1, genre2);
        assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("entity1 the foobar"), 10)).containsExactly(genre1);
        assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("other"), 10)).isEmpty();
    }

    @Test
    public void shouldSearchArtists() {

        Artist artist1 = artistRepository.save(buildArtist("the foobar entity1"));
        Artist artist2 = artistRepository.save(buildArtist("the foobar entity2"));

        assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("foo"), 10)).containsOnly(artist1, artist2);
        assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
        assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("ent th foo"), 10)).containsOnly(artist1, artist2);
        assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("entity1 the foobar"), 10)).containsExactly(artist1);
        assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("other"), 10)).isEmpty();
    }

    @Test
    public void shouldSearchAlbums() {

        Artist artist = artistRepository.save(new Artist());

        Album album1 = albumRepository.save(buildAlbum(artist, "the foobar entity1"));
        Album album2 = albumRepository.save(buildAlbum(artist, "the foobar entity2"));

        assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("foo"), 10)).containsOnly(album1, album2);
        assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
        assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("ent th foo"), 10)).containsOnly(album1, album2);
        assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("entity1 the foobar"), 10)).containsExactly(album1);
        assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("other"), 10)).isEmpty();
    }

    @Test
    public void shouldSearchSongs() {

        Genre genre = genreRepository.save(new Genre());
        Artist artist = artistRepository.save(new Artist());
        Album album = albumRepository.save(new Album().setArtist(artist));

        Song song1 = songRepository.save(buildSong("the foobar entity1", genre, album));
        Song song2 = songRepository.save(buildSong("the foobar entity2", genre, album));

        assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("foo"), 10)).containsOnly(song1, song2);
        assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
        assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("ent th foo"), 10)).containsOnly(song1, song2);
        assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("entity1 the foobar"), 10)).containsExactly(song1);
        assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("other"), 10)).isEmpty();
    }

    private Genre buildGenre(String name) {
        return new Genre().setName(name);
    }

    private Artist buildArtist(String name) {
        return new Artist().setName(name);
    }

    private Album buildAlbum(Artist artist, String name) {
        return new Album().setArtist(artist).setName(name);
    }

    private Song buildSong(String name, Genre genre, Album album) {
        return new Song()
                .setAlbum(album)
                .setGenre(genre)
                .setBitRate(128L)
                .setBitRateVariable(false)
                .setPath(name)
                .setFileType(FileType.of("text/plain", "txt"))
                .setDuration(666L)
                .setSize(256L)
                .setName(name);
    }
}
