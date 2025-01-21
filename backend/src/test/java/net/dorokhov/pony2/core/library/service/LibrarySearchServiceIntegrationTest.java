package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.LibrarySearchService;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.GenreRepository;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

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
    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    @Test
    public void shouldSearchGenres() {

        Genre genre1 = genreRepository.save(new Genre().setName("the foobar entity1"));
        Genre genre2 = genreRepository.save(new Genre().setName("the foobar entity2"));
        Genre genre3 = genreRepository.save(new Genre().setName("русская песня"));

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("foo"), 10)).containsOnly(genre1, genre2);
            assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("ащщ"), 10)).containsOnly(genre1, genre2);
            assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
            assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
            assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("ent th foo"), 10)).containsOnly(genre1, genre2);
            assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("entity1 the foobar"), 10)).containsExactly(genre1);
            assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("русс"), 10)).containsExactly(genre3);
            assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("hecc"), 10)).containsExactly(genre3);
            assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("РУСС"), 10)).containsExactly(genre3);
            assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("песня русская"), 10)).containsExactly(genre3);
            assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("рус пес"), 10)).containsExactly(genre3);
            assertThat(librarySearchService.searchGenres(LibrarySearchQuery.of("other"), 10)).isEmpty();
        });
    }

    @Test
    public void shouldSearchArtists() {

        Artist artist1 = artistRepository.save(new Artist().setName("the foobar entity1"));
        Artist artist2 = artistRepository.save(new Artist().setName("the foobar entity2"));

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("foo"), 10)).containsOnly(artist1, artist2);
            assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("ащщ"), 10)).containsOnly(artist1, artist2);
            assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
            assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
            assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("ent th foo"), 10)).containsOnly(artist1, artist2);
            assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("entity1 the foobar"), 10)).containsExactly(artist1);
            assertThat(librarySearchService.searchArtists(LibrarySearchQuery.of("other"), 10)).isEmpty();
        });
    }

    @Test
    public void shouldSearchAlbums() {

        Artist artist = artistRepository.save(new Artist().setName("artist"));

        Album album1 = albumRepository.save(new Album().setArtist(artist).setName("the foobar entity1").setYear(1991));
        Album album2 = albumRepository.save(new Album().setArtist(artist).setName("the foobar entity2").setYear(1999));

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("foo"), 10)).containsOnly(album1, album2);
            assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("ащщ"), 10)).containsOnly(album1, album2);
            assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
            assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
            assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("ent th foo"), 10)).containsOnly(album1, album2);
            assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("entity1 the foobar"), 10)).containsExactly(album1);
            assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("artist entity"), 10)).containsOnly(album1, album2);
            assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("artist entity 1999"), 10)).containsExactly(album2);
            assertThat(librarySearchService.searchAlbums(LibrarySearchQuery.of("other"), 10)).isEmpty();
        });
    }

    @Test
    public void shouldSearchSongs() {

        Genre genre = genreRepository.save(new Genre());
        Artist artist = artistRepository.save(new Artist());
        Album album = albumRepository.save(new Album().setArtist(artist));

        Song song1 = songRepository.save(new Song()
                .setAlbum(album)
                .setGenre(genre)
                .setBitRate(128L)
                .setBitRateVariable(false)
                .setPath("the foobar entity1")
                .setFileType(FileType.of("text/plain", "txt"))
                .setDuration(666L)
                .setSize(256L)
                .setName("the foobar entity1 underscore_separated_words")
                .setArtistName("артист")
                .setAlbumArtistName("другой")
                .setAlbumName("альбом")
        );
        Song song2 = songRepository.save(new Song()
                .setAlbum(album)
                .setGenre(genre)
                .setBitRate(128L)
                .setBitRateVariable(false)
                .setPath("the foobar entity2")
                .setFileType(FileType.of("text/plain", "txt"))
                .setDuration(666L)
                .setSize(256L)
                .setName("the foobar entity2 don'g b-52's гнёт-ель 1'33\"")
                .setArtistName("артист")
                .setAlbumArtistName("другой")
                .setAlbumName("альбом")
        );

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("foo"), 10)).containsOnly(song1, song2);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("ащщ"), 10)).containsOnly(song1, song2);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("foo"), 1)).hasSize(1);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("ent th foo"), 10)).containsOnly(song1, song2);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("entity1 the foobar"), 10)).containsExactly(song1);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("underscore_separated"), 10)).containsExactly(song1);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("underscore_separated_"), 10)).containsExactly(song1);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("underscore separated words"), 10)).containsExactly(song1);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("артист, другой . entity2"), 10)).containsExactly(song2);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("гнёт"), 10)).containsOnly(song2);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("гнет"), 10)).containsOnly(song2);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("uytn"), 10)).containsOnly(song2);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("гнет-ель"), 10)).containsOnly(song2);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("other"), 10)).isEmpty();
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("don'g"), 10)).containsOnly(song2);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("don g"), 10)).containsOnly(song2);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("dong"), 10)).containsOnly(song2);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("b-52's"), 10)).containsOnly(song2);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("b52s"), 10)).containsOnly(song2);
            assertThat(librarySearchService.searchSongs(LibrarySearchQuery.of("1'33\""), 10)).containsOnly(song2);
        });
    }
}
