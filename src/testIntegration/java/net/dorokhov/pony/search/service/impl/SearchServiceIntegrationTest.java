package net.dorokhov.pony.search.service.impl;

import net.dorokhov.pony.library.domain.Album;
import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.Genre;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.domain.FileType;
import net.dorokhov.pony.library.repository.AlbumRepository;
import net.dorokhov.pony.library.repository.ArtistRepository;
import net.dorokhov.pony.library.repository.GenreRepository;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.search.service.SearchService;
import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.search.domain.SearchQuery;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private SearchService searchService;

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

        assertThat(searchService.searchGenres(SearchQuery.of("foo"), 10)).containsOnly(genre1, genre2);
        assertThat(searchService.searchGenres(SearchQuery.of("foo"), 1)).hasSize(1);
        assertThat(searchService.searchGenres(SearchQuery.of("ent th foo"), 10)).containsOnly(genre1, genre2);
        assertThat(searchService.searchGenres(SearchQuery.of("entity1 the foobar"), 10)).containsExactly(genre1);
        assertThat(searchService.searchGenres(SearchQuery.of("other"), 10)).isEmpty();
    }

    @Test
    public void shouldSearchArtists() throws Exception {

        Artist artist1 = artistRepository.save(buildArtist("the foobar entity1"));
        Artist artist2 = artistRepository.save(buildArtist("the foobar entity2"));

        assertThat(searchService.searchArtists(SearchQuery.of("foo"), 10)).containsOnly(artist1, artist2);
        assertThat(searchService.searchArtists(SearchQuery.of("foo"), 1)).hasSize(1);
        assertThat(searchService.searchArtists(SearchQuery.of("ent th foo"), 10)).containsOnly(artist1, artist2);
        assertThat(searchService.searchArtists(SearchQuery.of("entity1 the foobar"), 10)).containsExactly(artist1);
        assertThat(searchService.searchArtists(SearchQuery.of("other"), 10)).isEmpty();
    }

    @Test
    public void shouldSearchAlbums() throws Exception {

        Artist artist = artistRepository.save(Artist.builder().build());

        Album album1 = albumRepository.save(buildAlbum(artist, "the foobar entity1"));
        Album album2 = albumRepository.save(buildAlbum(artist, "the foobar entity2"));

        assertThat(searchService.searchAlbums(SearchQuery.of("foo"), 10)).containsOnly(album1, album2);
        assertThat(searchService.searchAlbums(SearchQuery.of("foo"), 1)).hasSize(1);
        assertThat(searchService.searchAlbums(SearchQuery.of("ent th foo"), 10)).containsOnly(album1, album2);
        assertThat(searchService.searchAlbums(SearchQuery.of("entity1 the foobar"), 10)).containsExactly(album1);
        assertThat(searchService.searchAlbums(SearchQuery.of("other"), 10)).isEmpty();
    }

    @Test
    public void shouldSearchSongs() throws Exception {

        Genre genre = genreRepository.save(Genre.builder().build());
        Artist artist = artistRepository.save(Artist.builder().build());
        Album album = albumRepository.save(Album.builder().artist(artist).build());

        Song song1 = songRepository.save(buildSong("the foobar entity1", genre, album));
        Song song2 = songRepository.save(buildSong("the foobar entity2", genre, album));

        assertThat(searchService.searchSongs(SearchQuery.of("foo"), 10)).containsOnly(song1, song2);
        assertThat(searchService.searchSongs(SearchQuery.of("foo"), 1)).hasSize(1);
        assertThat(searchService.searchSongs(SearchQuery.of("ent th foo"), 10)).containsOnly(song1, song2);
        assertThat(searchService.searchSongs(SearchQuery.of("entity1 the foobar"), 10)).containsExactly(song1);
        assertThat(searchService.searchSongs(SearchQuery.of("other"), 10)).isEmpty();
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
