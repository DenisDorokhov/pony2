package net.dorokhov.pony2.web.controller;

import com.google.common.io.Files;
import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.web.dto.AuthenticationDto;
import net.dorokhov.pony2.web.dto.BackupDto;
import net.dorokhov.pony2.web.dto.RestoredHistoryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;

public class PlaybackHistoryAdminControllerTest extends InstallingIntegrationTest {

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
    public void shouldBackupPlaybackHistory() {

        PlaybackHistorySong historySong1 = playbackHistorySongRepository.save(new PlaybackHistorySong()
                .setCreationDate(LocalDateTime.now().minusDays(2))
                .setSong(song1_1_1)
                .setUser(user)
        );
        PlaybackHistorySong historySong2 = playbackHistorySongRepository.save(new PlaybackHistorySong()
                .setCreationDate(LocalDateTime.now().minusDays(1))
                .setSong(song1_1_2)
                .setUser(user)
        );

        ResponseEntity<BackupDto> backupResponse = apiTemplate.getRestTemplate().exchange(
                "/api/admin/history/backup", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), BackupDto.class);

        assertThat(backupResponse.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(backupResponse.getBody()).satisfies(dto -> {
            assertThat(dto.getFileContent()).isNotNull();
            assertThat(dto.getFileContent()).satisfies(backup -> {

                playbackHistorySongRepository.deleteAll();

                File file = tempFolder.resolve("history-backup").toFile();
                Files.write(backup.getBytes(StandardCharsets.UTF_8), file);

                MultiValueMap<String, Object> request = new LinkedMultiValueMap<>();
                request.add("file", new FileSystemResource(file));

                ResponseEntity<RestoredHistoryDto> restoreResponse = apiTemplate.getRestTemplate().exchange(
                        "/api/admin/history/restore", HttpMethod.POST,
                        apiTemplate.createHeaderRequest(request, authentication.getAccessToken(), new HttpHeaders()), RestoredHistoryDto.class);

                assertThat(restoreResponse.getStatusCode()).isSameAs(HttpStatus.OK);
                assertThat(restoreResponse.getBody()).satisfies(restoredHistory -> {

                    assertThat(restoredHistory.getRestoredSongCount()).isEqualTo(2);
                    assertThat(restoredHistory.getNotFoundSongCount()).isZero();
                    assertThat(restoredHistory.getNotFoundUserEmails()).isEmpty();

                    List<PlaybackHistorySong> history = playbackHistorySongRepository.findAll().stream()
                            .sorted(Comparator.comparing(PlaybackHistorySong::getCreationDate))
                            .toList();

                    assertThat(history).hasSize(2);
                    assertThat(history).first().satisfies(song -> {
                        assertThat(song.getSong().getId()).isEqualTo(song1_1_1.getId());
                        assertThat(song.getCreationDate()).isEqualTo(historySong1.getCreationDate());
                    });
                    assertThat(history).last().satisfies(song -> {
                        assertThat(song.getSong().getId()).isEqualTo(song1_1_2.getId());
                        assertThat(song.getCreationDate()).isEqualTo(historySong2.getCreationDate());
                    });
                });
            });
        });
    }
}
