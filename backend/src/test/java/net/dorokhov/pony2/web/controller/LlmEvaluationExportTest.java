package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.api.user.service.command.UserCreationCommand;
import net.dorokhov.pony2.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.web.dto.AuthenticationDto;
import net.dorokhov.pony2.web.dto.ErrorDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "pony.home=${user.dir}/build/llm-evaluation-test")
public class LlmEvaluationExportTest extends InstallingIntegrationTest {

    private static final String PATH = "/api/admin/llm/evaluation";
    private static final LocalDateTime CREATED = LocalDateTime.of(2020, 1, 1, 0, 0);
    private static final String RAW_REQUEST = "[{\"prompt\":\"BjГ¶rk \\n \\\"quoted\\\"\"}]";
    private static final String RAW_RESULT = "[\"An unstructured response: ж—Ґжњ¬иЄћ\",null]";

    @Autowired
    private ApiTemplate apiTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JsonMapper jsonMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private LibraryJobSynchronizer jobSynchronizer;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldRejectEvaluationDuringDiscoveryOrScanWithoutCancellation(boolean discovery) throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        HttpEntity<?> request = apiTemplate.createHeaderRequest(authentication.getAccessToken());
        try (LibraryJobSynchronizer.LibraryJobRegistration ignored = discovery
                ? jobSynchronizer.registerDiscoveryJob() : jobSynchronizer.registerScanJob(Duration.ZERO)) {
            boolean previouslyCancelled = jobSynchronizer.isDiscoveryCancelled();

            ResponseEntity<String> response = download(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(jsonMapper.readTree(response.getBody()).get("code").asString())
                    .isEqualTo(ErrorDto.Code.CONCURRENT_LIBRARY_JOB.name());
            assertThat(response.getHeaders().getFirst("Content-Disposition")).isNull();
            assertThat(jobSynchronizer.isDiscoveryCancelled()).isEqualTo(previouslyCancelled);
        }

        assertThat(download(request).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldExportLatestSuccessfulTasksForEveryTargetKindWithMetadata() {
        insertArtist(1, "Bj\u00f6rk \"quoted\"\n\u65e5\u672c\u8a9e");
        insertAlbum(1, 1);
        for (int number = 1; number <= 103; number++) {
            jdbcTemplate.update("INSERT INTO genre (id, creation_date, name) VALUES (?, ?, ?)", id(number), CREATED, "Genre " + number);
            jdbcTemplate.update("INSERT INTO artist_genre (id, creation_date, artist_id, genre_id) VALUES (?, ?, ?, ?)",
                    id(number), CREATED, id(1), id(number));
        }
        insertSong(1);
        insertJob(1);
        insertJob(2);
        jdbcTemplate.update("UPDATE discovery_job SET status = 'FAILED' WHERE id = ?", id(2));
        for (Kind kind : Kind.values()) {
            int offset = (kind.ordinal() + 1) * 100;
            insertTask(kind, offset, 1, 1, "SPOTIFY_ARTIST_DATA", "COMPLETE", CREATED, "{\"old\":true}");
            insertTask(kind, offset + 1, 1, 2, "SPOTIFY_ARTIST_DATA", "COMPLETE", CREATED.plusMinutes(1), "{\"selected\":true}");
            insertTask(kind, offset + 2, 1, 2, "SPOTIFY_ARTIST_DATA", "FAILED", CREATED.plusMinutes(2), "failure");
            insertTask(kind, offset + 3, 1, 1, "SPOTIFY_TOP_TRACKS", "COMPLETE", CREATED, "[]");
            insertTask(kind, offset + 4, 1, 2, "SPOTIFY_ARTIST_DATA", "COMPLETE", CREATED.plusMinutes(1), "{\"selected\":true}");
            insertTask(kind, offset + 5, 1, 1, "MUSICBRAINZ_ALBUM_DATA", "COMPLETE", CREATED, "{\"musicBrainz\":true}");
        }

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<String> response = download(apiTemplate.createCookieRequest(authentication.getStaticToken()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo("inline");
        assertThat(response.getHeaders().getContentDisposition().getFilename()).startsWith("pony-evaluation-").endsWith(".json");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(-1);
        JsonNode document = jsonMapper.readTree(response.getBody());
        assertThat(document.properties()).hasSize(5);
        for (Kind kind : Kind.values()) {
            JsonNode objects = document.get(kind.entityName() + "s");
            assertThat(objects.size()).isEqualTo(1);
            JsonNode object = objects.get(0);
            assertThat(object.get("id").asString()).isEqualTo(id(1));
            JsonNode tasks = object.get("tasks");
            assertThat(tasks.size()).isEqualTo(3);
            assertThat(tasks.get(0).get("type").asString()).isEqualTo("MUSICBRAINZ_ALBUM_DATA");
            assertThat(tasks.get(0).get("result").get("musicBrainz").asBoolean()).isTrue();
            assertThat(tasks.get(1).get("type").asString()).isEqualTo("SPOTIFY_ARTIST_DATA");
            assertThat(tasks.get(1).get("id").asString()).isEqualTo(id((kind.ordinal() + 1) * 100 + 4));
            assertThat(tasks.get(1).get("jobId").asString()).isEqualTo(id(2));
            assertThat(tasks.get(1).has("parameter")).isFalse();
            assertThat(tasks.get(1).get("result").get("selected").asBoolean()).isTrue();
            assertThat(tasks.get(2).get("type").asString()).isEqualTo("SPOTIFY_TOP_TRACKS");
            assertThat(tasks.get(2).get("result").isArray()).isTrue();
            for (JsonNode task : tasks) {
                assertThat(task.get("status").asString()).isEqualTo("COMPLETE");
                assertThat(task.get("rawRequest").asString()).isEqualTo(RAW_REQUEST);
                assertThat(task.get("rawResult").asString()).isEqualTo(RAW_RESULT);
            }
        }
        JsonNode artist = document.get("artists").get(0);
        assertThat(artist.get("genres").size()).isEqualTo(103);
        assertThat(artist.get("genres").get(102).get("name").asString()).isEqualTo("Genre 103");
        assertThat(artist.get("name").asString()).isEqualTo("Bj\u00f6rk \"quoted\"\n\u65e5\u672c\u8a9e");
        JsonNode album = document.get("albums").get(0);
        assertThat(album.get("year").intValue()).isEqualTo(2020);
        assertThat(album.get("artist").get("id").asString()).isEqualTo(id(1));
        JsonNode song = document.get("songs").get(0);
        assertThat(song.get("album").get("id").asString()).isEqualTo(id(1));
        assertThat(song.get("genre").get("id").asString()).isEqualTo(id(1));
        assertThat(song.get("trackNumber").intValue()).isEqualTo(2);
    }

    @Test
    void shouldIgnoreFailuresAndLaterUpdatesAndBreakCreationDateTiesById() {
        insertArtist(1, "Artist");
        insertArtist(2, "Only failed tasks");
        insertJob(1);
        insertTask(Kind.ARTIST, 10, 1, 1, "SPOTIFY_ARTIST_DATA", "COMPLETE", CREATED, "{}");
        insertTask(Kind.ARTIST, 11, 1, 1, "SPOTIFY_ARTIST_DATA", "COMPLETE", CREATED, null);
        insertTask(Kind.ARTIST, 12, 1, 1, "SPOTIFY_ARTIST_DATA", "COMPLETE", CREATED.minusDays(1), "{}");
        insertTask(Kind.ARTIST, 13, 1, 1, "SPOTIFY_ARTIST_DATA", "INTERRUPTED", CREATED.plusDays(1), "{}");
        insertTask(Kind.ARTIST, 14, 2, 1, "SPOTIFY_ARTIST_DATA", "FAILED", CREATED, "{}");
        jdbcTemplate.update("UPDATE discovery_task SET update_date = ? WHERE id = ?", LocalDateTime.now().plusDays(1), id(10));

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        JsonNode document = jsonMapper.readTree(download(apiTemplate.createHeaderRequest(authentication.getAccessToken())).getBody());

        assertThat(document.get("artists").size()).isEqualTo(1);
        JsonNode task = document.get("artists").get(0).get("tasks").get(0);
        assertThat(task.get("id").asString()).isEqualTo(id(11));
        assertThat(task.has("result")).isFalse();
        assertThat(document.get("albums").isEmpty()).isTrue();
    }

    @Test
    void shouldExportAlbumContextWithoutArtistTasksAndPreserveLargeRawData() {
        insertArtist(1, null);
        insertAlbum(1, 1);
        jdbcTemplate.update("UPDATE album SET name = NULL, album_year = NULL WHERE id = ?", id(1));
        insertJob(1);
        insertTask(Kind.ALBUM, 1, 1, 1, "MUSICBRAINZ_ALBUM_DATA", "COMPLETE", CREATED, "{}");
        String rawRequest = "Full prompt ".repeat(8192);
        String rawResult = "Full response ".repeat(8192);
        jdbcTemplate.update("UPDATE discovery_task SET raw_request = ?, raw_result = ? WHERE id = ?", rawRequest, rawResult, id(1));

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        JsonNode document = jsonMapper.readTree(download(apiTemplate.createHeaderRequest(authentication.getAccessToken())).getBody());

        assertThat(document.get("artists").isEmpty()).isTrue();
        JsonNode album = document.get("albums").get(0);
        assertThat(album.get("name").isNull()).isTrue();
        assertThat(album.get("year").isNull()).isTrue();
        assertThat(album.get("artist").get("name").isNull()).isTrue();
        assertThat(album.get("tasks").get(0).get("rawRequest").asString()).isEqualTo(rawRequest);
        assertThat(album.get("tasks").get(0).get("rawResult").asString()).isEqualTo(rawResult);
    }

    @Test
    void shouldExportMultipleEntityPagesWithoutDuplicates() {
        insertJob(1);
        for (int number = 1; number <= 310; number++) {
            insertArtist(number, "Artist " + number);
            insertAlbum(number, number);
            if (number > 105) {
                insertTask(Kind.ARTIST, number, number, 1, "SPOTIFY_ARTIST_DATA", "COMPLETE", CREATED, "{}");
                insertTask(Kind.ALBUM, number + 1000, number, 1, "MUSICBRAINZ_ALBUM_DATA", "COMPLETE", CREATED, "{}");
            }
        }

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        JsonNode document = jsonMapper.readTree(download(apiTemplate.createHeaderRequest(authentication.getAccessToken())).getBody());

        for (String collection : List.of("artists", "albums")) {
            JsonNode objects = document.get(collection);
            assertThat(objects.size()).isEqualTo(205);
            for (int index = 0; index < 205; index++) {
                assertThat(objects.get(index).get("id").asString()).isEqualTo(id(index + 106));
            }
        }
    }

    @Test
    void shouldRejectAnonymousAndNonAdminDownloadsAndKeepOtherAdminRoutesProtected() throws DuplicateEmailException {
        assertThat(download(HttpEntity.EMPTY).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        User user = userService.create(new UserCreationCommand().setName("User").setEmail("user@example.com")
                .setPassword("foobar").setRoles(Set.of(User.Role.USER)));
        AuthenticationDto userAuthentication = apiTemplate.authenticate(user.getEmail(), "foobar");
        assertThat(download(apiTemplate.createCookieRequest(userAuthentication.getStaticToken())).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(download(apiTemplate.createHeaderRequest(userAuthentication.getAccessToken())).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        AuthenticationDto adminAuthentication = apiTemplate.authenticateAdmin();
        assertThat(apiTemplate.getRestTemplate().exchange("/api/admin/config", HttpMethod.GET,
                apiTemplate.createCookieRequest(adminAuthentication.getStaticToken()), String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(apiTemplate.getRestTemplate().exchange(PATH, HttpMethod.POST,
                apiTemplate.createCookieRequest(adminAuthentication.getStaticToken()), String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<String> download(HttpEntity<?> request) {
        return apiTemplate.getRestTemplate().exchange(PATH, HttpMethod.GET, request, String.class);
    }

    private void insertArtist(int number, String name) {
        jdbcTemplate.update("INSERT INTO artist (id, creation_date, name) VALUES (?, ?, ?)", id(number), CREATED, name);
    }

    private void insertAlbum(int number, int artist) {
        jdbcTemplate.update("INSERT INTO album (id, creation_date, name, album_year, artist_id) VALUES (?, ?, ?, ?, ?)",
                id(number), CREATED, "Album " + number, 2020, id(artist));
    }

    private void insertSong(int number) {
        jdbcTemplate.update("""
                INSERT INTO song (id, creation_date, path, mime_type, file_extension, size, duration,
                                  bit_rate, bit_rate_variable, name, song_year, disc_number, track_number,
                                  artist_id, album_id, genre_id)
                VALUES (?, ?, ?, 'audio/mpeg', 'mp3', 100, 120, 320, FALSE, 'Song', 2020, 1, 2, ?, ?, ?)
                """, id(number), CREATED, "song-" + number + ".mp3", id(1), id(1), id(1));
    }

    private void insertJob(int number) {
        jdbcTemplate.update("INSERT INTO discovery_job (id, creation_date, discovery_type, status) VALUES (?, ?, 'FULL', 'COMPLETE')",
                id(number), CREATED);
    }

    private void insertTask(Kind kind, int number, int entity, int job, String type, String status, LocalDateTime date, String result) {
        jdbcTemplate.update("""
                INSERT INTO discovery_task (id, creation_date, update_date, discovery_job_id, type, status,
                                            result, raw_request, raw_result, %s_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.formatted(kind.entityName()), id(number), date, date.plusMinutes(1), id(job), type, status,
                result, RAW_REQUEST, RAW_RESULT, id(entity));
    }

    private static String id(int number) {
        return "00000000-0000-0000-0000-%012d".formatted(number);
    }

    private enum Kind {
        GENRE("genre"), ARTIST("artist"), ALBUM("album"), SONG("song");

        private final String entityName;

        Kind(String entityName) {
            this.entityName = entityName;
        }

        String entityName() {
            return entityName;
        }
    }
}
