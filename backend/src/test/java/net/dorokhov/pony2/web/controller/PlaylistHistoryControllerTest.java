package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.web.dto.AuthenticationDto;
import net.dorokhov.pony2.web.dto.PlaybackHistorySongDto;
import net.dorokhov.pony2.web.dto.SongDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;

public class PlaylistHistoryControllerTest extends InstallingIntegrationTest {

    @Autowired
    private ApiTemplate apiTemplate;

    @Autowired
    private UserService userService;
    @Autowired
    private PlaybackHistorySongRepository playbackHistorySongRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private SongRepository songRepository;

    private AuthenticationDto authentication;
    private User user;

    private Genre genre1;
    private Genre genre2;

    private Artist artist1;
    private Artist artist2;

    private Album album1_1;
    private Album album1_2;
    private Album album2_1;

    private Song song1_1_1;
    private Song song1_1_2;
    private Song song1_2_1;
    private Song song2_1_1;

    @BeforeEach
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void setUp() {

        authentication = apiTemplate.authenticateAdmin();
        user = userService.getById(authentication.getUser().getId()).orElseThrow();

        getTransactionTemplate().executeWithoutResult(transactionStatus -> {

            genre1 = genreRepository.save(new Genre()
                    .setName("foo genre1"));
            genre2 = genreRepository.save(new Genre()
                    .setName("foo genre2"));

            artist1 = artistRepository.save(new Artist()
                    .setName("foo artist1"));
            artist2 = artistRepository.save(new Artist()
                    .setName("foo artist2"));

            album1_1 = albumRepository.save(new Album()
                    .setYear(1986)
                    .setName("foo album1_1")
                    .setArtist(artist1));
            album1_2 = albumRepository.save(new Album()
                    .setYear(1986)
                    .setName("foo album1_2")
                    .setArtist(artist1));
            album2_1 = albumRepository.save(new Album()
                    .setYear(1986)
                    .setName("foo album2_1")
                    .setArtist(artist2));

            song1_1_1 = songRepository.save(song()
                    .setId(null)
                    .setCreationDate(null)
                    .setUpdateDate(null)
                    .setArtistName("bar")
                    .setPath("song1_1_1")
                    .setName("foo song1_1_1")
                    .setAlbum(album1_1)
                    .setGenre(genre1));
            song1_1_1.getAlbum().getArtist(); // Pre-fetch.
            song1_1_1.getGenre(); // Pre-fetch.

            song1_1_2 = songRepository.save(song()
                    .setId(null)
                    .setCreationDate(null)
                    .setUpdateDate(null)
                    .setArtistName("bar")
                    .setPath("song1_1_2")
                    .setName("foo song1_1_2")
                    .setAlbum(album1_1)
                    .setGenre(genre1));
            song1_1_2.getAlbum().getArtist(); // Pre-fetch.
            song1_1_2.getGenre(); // Pre-fetch.

            song1_2_1 = songRepository.save(song()
                    .setId(null)
                    .setCreationDate(null)
                    .setUpdateDate(null)
                    .setArtistName("bar")
                    .setPath("song1_2_1")
                    .setName("foo song1_2_1")
                    .setAlbum(album1_2)
                    .setGenre(genre1));
            song1_2_1.getAlbum().getArtist(); // Pre-fetch.
            song1_2_1.getGenre(); // Pre-fetch.

            song2_1_1 = songRepository.save(song()
                    .setId(null)
                    .setCreationDate(null)
                    .setUpdateDate(null)
                    .setArtistName("bar")
                    .setPath("song2_1_1")
                    .setName("foo song2_1_1")
                    .setAlbum(album2_1)
                    .setGenre(genre2));
            song2_1_1.getAlbum().getArtist(); // Pre-fetch.
            song2_1_1.getGenre(); // Pre-fetch.
        });
    }

    @Test
    public void shouldGetHistory() throws InterruptedException {

        playbackHistorySongRepository.save(new PlaybackHistorySong()
                .setSong(song1_1_1)
                .setUser(user));
        Thread.sleep(100);
        playbackHistorySongRepository.save(new PlaybackHistorySong()
                .setSong(song1_1_2)
                .setUser(user));
        Thread.sleep(100);
        playbackHistorySongRepository.save(new PlaybackHistorySong()
                .setSong(song1_2_1)
                .setUser(user));

        ResponseEntity<PlaybackHistorySongDto[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/history", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaybackHistorySongDto[].class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(history -> {
            assertThat(history).hasSize(3);
            assertThat(history[0]).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                assertThat(song.getCreationDate()).isNotNull();
                checkSongDto(song.getSong(), song1_2_1);
            });
            assertThat(history[1]).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                assertThat(song.getCreationDate()).isNotNull();
                checkSongDto(song.getSong(), song1_1_2);
            });
            assertThat(history[2]).satisfies(song -> {
                assertThat(song.getId()).isNotNull();
                assertThat(song.getCreationDate()).isNotNull();
                checkSongDto(song.getSong(), song1_1_1);
            });
        });
    }

    private void checkSongDto(SongDto dto, Song song) {
        assertThat(dto.getId()).isEqualTo(song.getId());
        assertThat(dto.getCreationDate()).isEqualTo(song.getCreationDate());
        assertThat(dto.getUpdateDate()).isEqualTo(song.getUpdateDate());
        assertThat(dto.getMimeType()).isEqualTo(song.getFileType().getMimeType());
        assertThat(dto.getFileExtension()).isEqualTo(song.getFileType().getFileExtension());
        assertThat(dto.getSize()).isEqualTo(song.getSize());
        assertThat(dto.getDuration()).isEqualTo(song.getDuration());
        assertThat(dto.getBitRate()).isEqualTo(song.getBitRate());
        assertThat(dto.getBitRateVariable()).isEqualTo(song.getBitRateVariable());
        assertThat(dto.getDiscNumber()).isEqualTo(1);
        assertThat(dto.getTrackNumber()).isEqualTo(song.getTrackNumber());
        assertThat(dto.getName()).isEqualTo(song.getName());
        assertThat(dto.getArtistName()).isEqualTo("bar");
        assertThat(dto.getGenreName()).isEqualTo(song.getGenreName());
        assertThat(dto.getAlbumId()).isEqualTo(song.getAlbum().getId());
        assertThat(dto.getGenreId()).isEqualTo(song.getGenre().getId());
    }

    @Test
    public void shouldAddSongToHistory() {

        ResponseEntity<PlaybackHistorySongDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/history/{songId}", HttpMethod.POST,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaybackHistorySongDto.class, song1_1_1.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(playbackHistorySong -> {
            assertThat(playbackHistorySong.getId()).isNotNull();
            assertThat(playbackHistorySong.getCreationDate()).isNotNull();
            checkSongDto(playbackHistorySong.getSong(), song1_1_1);
        });
    }

    @Test
    public void shouldFailAddingNotExistingSongToHistory() {

        ResponseEntity<PlaybackHistorySongDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/history/{songId}", HttpMethod.POST,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), PlaybackHistorySongDto.class, "foobar");

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
    }
}
