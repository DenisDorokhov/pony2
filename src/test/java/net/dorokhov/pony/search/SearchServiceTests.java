package net.dorokhov.pony.search;

import net.dorokhov.pony.entity.Album;
import net.dorokhov.pony.entity.Artist;
import net.dorokhov.pony.entity.Genre;
import net.dorokhov.pony.entity.Song;
import net.dorokhov.pony.repository.AlbumRepository;
import net.dorokhov.pony.repository.ArtistRepository;
import net.dorokhov.pony.repository.GenreRepository;
import net.dorokhov.pony.repository.SongRepository;
import net.dorokhov.pony.test.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchServiceTests extends IntegrationTest {
    
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
    public void searchGenres() throws Exception {

        Genre genre1 = transactionTemplate.execute(status -> genreRepository.save(buildGenre("the foobar entity1")));
        Genre genre2 = transactionTemplate.execute(status -> genreRepository.save(buildGenre("the foobar entity2")));
        
        assertThat(searchService.searchGenres("foo", 10)).containsOnly(genre1, genre2);
        assertThat(searchService.searchGenres("foo", 1)).hasSize(1);
        assertThat(searchService.searchGenres("ent th foo", 10)).containsOnly(genre1, genre2);
        assertThat(searchService.searchGenres("entity1 the foobar", 10)).containsExactly(genre1);
        assertThat(searchService.searchGenres("other", 10)).isEmpty();
    }

    @Test
    public void searchArtists() throws Exception {

        Artist artist1 = transactionTemplate.execute(status -> artistRepository.save(buildArtist("the foobar entity1")));
        Artist artist2 = transactionTemplate.execute(status -> artistRepository.save(buildArtist("the foobar entity2")));

        assertThat(searchService.searchArtists("foo", 10)).containsOnly(artist1, artist2);
        assertThat(searchService.searchArtists("foo", 1)).hasSize(1);
        assertThat(searchService.searchArtists("ent th foo", 10)).containsOnly(artist1, artist2);
        assertThat(searchService.searchArtists("entity1 the foobar", 10)).containsExactly(artist1);
        assertThat(searchService.searchArtists("other", 10)).isEmpty();
    }
    
    @Test
    public void searchAlbums() throws Exception {

        Artist artist = transactionTemplate.execute(status -> artistRepository.save(new Artist()));
        
        Album album1 = transactionTemplate.execute(status -> albumRepository.save(buildAlbum(artist, "the foobar entity1")));
        Album album2 = transactionTemplate.execute(status -> albumRepository.save(buildAlbum(artist, "the foobar entity2")));

        assertThat(searchService.searchAlbums("foo", 10)).containsOnly(album1, album2);
        assertThat(searchService.searchAlbums("foo", 1)).hasSize(1);
        assertThat(searchService.searchAlbums("ent th foo", 10)).containsOnly(album1, album2);
        assertThat(searchService.searchAlbums("entity1 the foobar", 10)).containsExactly(album1);
        assertThat(searchService.searchAlbums("other", 10)).isEmpty();
    }

    @Test
    public void searchSongs() throws Exception {

        Genre genre = transactionTemplate.execute(status -> genreRepository.save(new Genre()));
        Artist artist = transactionTemplate.execute(status -> artistRepository.save(new Artist()));
        Album album = transactionTemplate.execute(status -> albumRepository.save(new Album(artist)));

        Song song1 = transactionTemplate.execute(status -> songRepository.save(buildSong("the foobar entity1", genre, album)));
        Song song2 = transactionTemplate.execute(status -> songRepository.save(buildSong("the foobar entity2", genre, album)));

        assertThat(searchService.searchSongs("foo", 10)).containsOnly(song1, song2);
        assertThat(searchService.searchSongs("foo", 1)).hasSize(1);
        assertThat(searchService.searchSongs("ent th foo", 10)).containsOnly(song1, song2);
        assertThat(searchService.searchSongs("entity1 the foobar", 10)).containsExactly(song1);
        assertThat(searchService.searchSongs("other", 10)).isEmpty();
    }

    @Test
    public void purgeIndex() throws Exception {

        Genre genre = transactionTemplate.execute(status -> genreRepository.save(buildGenre("entity")));
        Artist artist = transactionTemplate.execute(status -> artistRepository.save(buildArtist("entity")));
        Album album = transactionTemplate.execute(status -> albumRepository.save(buildAlbum(artist, "entity")));
        
        transactionTemplate.execute(status -> songRepository.save(buildSong("entity", genre, album)));
        
        transactionTemplate.execute(status -> {
            searchService.purgeIndex();
            return null;
        });
        
        assertThat(searchService.searchGenres("entity", 10)).isEmpty();
        assertThat(searchService.searchArtists("entity", 10)).isEmpty();
        assertThat(searchService.searchAlbums("entity", 10)).isEmpty();
        assertThat(searchService.searchSongs("entity", 10)).isEmpty();
    }

    private Genre buildGenre(String name) {
        Genre genre = new Genre();
        genre.setName(name);
        return genre;
    }

    private Artist buildArtist(String name) {
        Artist artist = new Artist();
        artist.setName(name);
        return artist;
    }

    private Album buildAlbum(Artist artist, String name) {
        Album album = new Album(artist);
        album.setName(name);
        return album;
    }

    private Song buildSong(String name, Genre genre, Album album) {
        Song song = new Song(album, genre);
        song.setBitRate(128L);
        song.setBitRateVariable(false);
        song.setPath(name);
        song.setMimeType("text/plain");
        song.setDuration(666L);
        song.setSize(256L);
        song.setName(name);
        return song;
    }
}
