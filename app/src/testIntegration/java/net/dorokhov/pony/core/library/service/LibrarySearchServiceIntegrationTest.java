package net.dorokhov.pony.core.library.service;

import net.dorokhov.pony.app.IntegrationTest;
import net.dorokhov.pony.api.library.domain.Album;
import net.dorokhov.pony.api.library.domain.Artist;
import net.dorokhov.pony.api.library.domain.FileType;
import net.dorokhov.pony.api.library.domain.Genre;
import net.dorokhov.pony.api.library.domain.LibrarySearchQuery;
import net.dorokhov.pony.api.library.domain.Song;
import net.dorokhov.pony.api.library.service.LibrarySearchService;
import net.dorokhov.pony.core.library.repository.AlbumRepository;
import net.dorokhov.pony.core.library.repository.ArtistRepository;
import net.dorokhov.pony.core.library.repository.GenreRepository;
import net.dorokhov.pony.core.library.repository.SongRepository;
import org.junit.Test;
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
    public void shouldSearchGenres() throws Exception {

        Genre genre1 = genreRepository.save(buildGenre("the foobar entity1"));
        Genre genre2 = genreRepository.save(buildGenre("the foobar entity2"));

        assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("foo"), 10)).containsOnly(genre1, genre2);
        assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
        assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("ent th foo"), 10)).containsOnly(genre1, genre2);
        assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("entity1 the foobar"), 10)).containsExactly(genre1);
        assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("other"), 10)).isEmpty();
    }

    @Test
    public void shouldSearchArtists() throws Exception {

        Artist artist1 = artistRepository.save(buildArtist("the foobar entity1"));
        Artist artist2 = artistRepository.save(buildArtist("the foobar entity2"));

        assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("foo"), 10)).containsOnly(artist1, artist2);
        assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
        assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("ent th foo"), 10)).containsOnly(artist1, artist2);
        assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("entity1 the foobar"), 10)).containsExactly(artist1);
        assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("other"), 10)).isEmpty();
    }

    @Test
    public void shouldSearchAlbums() throws Exception {

        Artist artist = artistRepository.save(Artist.builder().build());

        Album album1 = albumRepository.save(buildAlbum(artist, "the foobar entity1"));
        Album album2 = albumRepository.save(buildAlbum(artist, "the foobar entity2"));

        assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("foo"), 10)).containsOnly(album1, album2);
        assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
        assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("ent th foo"), 10)).containsOnly(album1, album2);
        assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("entity1 the foobar"), 10)).containsExactly(album1);
        assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("other"), 10)).isEmpty();
    }

    @Test
    public void shouldSearchSongs() throws Exception {

        Genre genre = genreRepository.save(Genre.builder().build());
        Artist artist = artistRepository.save(Artist.builder().build());
        Album album = albumRepository.save(Album.builder().artist(artist).build());

        Song song1 = songRepository.save(buildSong("the foobar entity1", genre, album));
        Song song2 = songRepository.save(buildSong("the foobar entity2", genre, album));

        assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("foo"), 10)).containsOnly(song1, song2);
        assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
        assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("ent th foo"), 10)).containsOnly(song1, song2);
        assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("entity1 the foobar"), 10)).containsExactly(song1);
        assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("other"), 10)).isEmpty();
    }

    private Genre buildGenre(String name) {
        return Genre.builder().name(name).build();
    }

    private Artist buildArtist(String name) {
        return Artist.builder().name(name).build();
    }

    private Album buildAlbum(Artist artist, String name) {
        return Album.builder().artist(artist).name(name).build();
    }

    private Song buildSong(String name, Genre genre, Album album) {
        return Song.builder()
                .album(album)
                .genre(genre)
                .bitRate(128L)
                .bitRateVariable(false)
                .path(name)
                .fileType(FileType.of("text/plain", "txt"))
                .duration(666L)
                .size(256L)
                .name(name)
                .build();
    }
}
