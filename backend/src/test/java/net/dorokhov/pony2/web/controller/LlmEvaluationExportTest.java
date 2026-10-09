package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.api.user.service.command.UserCreationCommand;
import net.dorokhov.pony2.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.web.service.LlmEvaluationExportService.Kind;
import net.dorokhov.pony2.web.dto.AuthenticationDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@TestPropertySource(properties = "pony.home=${user.dir}/build/llm-evaluation-test")
public class LlmEvaluationExportTest extends InstallingIntegrationTest {

    private static final String PATH = "/api/admin/llm/evaluation";
    private static final LocalDateTime CREATED = LocalDateTime.of(2020, 1, 1, 0, 0);
    private static final String RAW_REQUEST = "[{\"prompt\":\"Björk \\n \\\"quoted\\\"\"}]";
    private static final String RAW_RESULT = "[\"An unstructured response: 日本語\",null]";

    @Autowired
    private ApiTemplate apiTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JsonMapper jsonMapper;
    @Autowired
    private UserService userService;
    @MockitoSpyBean
    private ArtistRepository artistRepository;

    @Test
    void shouldExportLatestDiscoveriesWithMetadataAndEveryTaskUsingAdminCookie() {
        insertArtist(1, "Björk \"quoted\"\n日本語");
        insertArtist(2, null);
        insertArtist(3, "No discovery");
        insertAlbum(1, 1);
        insertAlbum(2, 1);
        insertAlbum(3, 1);
        insertJob(1);
        insertJob(2);
        insertJob(3);
        insertJob(4);
        for (Kind kind : Kind.values()) {
            insertDiscovery(kind, 99, 1, 1, CREATED);
            insertDiscovery(kind, 11, 1, 2, CREATED.plusDays(1));
            insertDiscovery(kind, 12, 1, 3, CREATED.plusDays(1));
            insertDiscovery(kind, 13, 1, 4, LocalDateTime.of(2999, 1, 1, 0, 0));
            insertDiscovery(kind, 14, 2, 1, CREATED);
            insertTask(kind, kind == Kind.ARTIST ? 999 : 1999, 99, 1, "COMPLETE", "old result");
            for (int index = 0; index < 19; index++) {
                String status = new String[]{"COMPLETE", "FAILED", "INTERRUPTED", "STARTED"}[index % 4];
                insertTask(kind, (kind == Kind.ARTIST ? 100 : 200) + index, 12, 3, status, index == 0 ? null : "{\"value\":1}");
            }
            int futureTask = kind == Kind.ARTIST ? 3000 : 4000;
            insertTask(kind, futureTask, 12, 3, "COMPLETE", "future result");
            jdbcTemplate.update("UPDATE discovery_task SET creation_date = ? WHERE id = ?",
                    LocalDateTime.of(2999, 1, 1, 0, 0), id(futureTask));
        }
        for (int index = 1; index <= 103; index++) {
            jdbcTemplate.update("INSERT INTO genre (id, creation_date, name) VALUES (?, ?, ?)",
                    id(index), CREATED, "Genre " + index);
            jdbcTemplate.update("INSERT INTO artist_genre (id, creation_date, artist_id, genre_id) VALUES (?, ?, ?, ?)",
                    id(index), CREATED, id(1), id(index));
        }

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<String> response = download(apiTemplate.createCookieRequest(authentication.getStaticToken()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo("attachment");
        assertThat(response.getHeaders().getContentDisposition().getFilename()).startsWith("pony-evaluation-").endsWith(".json");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(-1);
        JsonNode document = jsonMapper.readTree(response.getBody());
        assertThat(document.get("complete").booleanValue()).isTrue();
        for (Kind kind : Kind.values()) {
            JsonNode discoveries = document.get(kind.entityName() + "Discoveries");
            assertThat(discoveries.size()).isEqualTo(2);
            JsonNode latest = discoveries.get(0);
            assertThat(latest.get("id").asString()).isEqualTo(id(12));
            assertThat(latest.get("jobId").asString()).isEqualTo(id(3));
            assertThat(latest.get("creationDate").asString()).isEqualTo(CREATED.plusDays(1).toString());
            assertThat(latest.get("updateDate").isNull()).isTrue();
            JsonNode tasks = latest.get("tasks");
            assertThat(tasks.size()).isEqualTo(19);
            assertThat(tasks.get(0).get("result").isNull()).isTrue();
            for (JsonNode task : tasks) {
                assertThat(task.get("rawRequest").asString()).isEqualTo(RAW_REQUEST);
                assertThat(task.get("rawResult").asString()).isEqualTo(RAW_RESULT);
                assertThat(task.get("parameter").asString()).isEqualTo("{\"artistId\":\"" + id(1) + "\"}");
                assertThat(task.get("type").asString()).isEqualTo("SPOTIFY_ARTIST_DATA");
                assertThat(task.get("jobId").asString()).isEqualTo(id(3));
                assertThat(task.get("updateDate").asString()).isEqualTo(CREATED.plusMinutes(1).toString());
            }
            assertThat(tasks.get(1).get("status").asString()).isEqualTo("FAILED");
            assertThat(tasks.get(2).get("status").asString()).isEqualTo("INTERRUPTED");
            assertThat(tasks.get(3).get("status").asString()).isEqualTo("STARTED");
            assertThat(discoveries.get(1).get("tasks").isEmpty()).isTrue();
        }
        JsonNode artist = document.get("artistDiscoveries").get(0).get("artist");
        assertThat(artist.get("name").asString()).isEqualTo("Björk \"quoted\"\n日本語");
        assertThat(artist.get("genres").size()).isEqualTo(103);
        assertThat(artist.get("genres").get(102).get("name").asString()).isEqualTo("Genre 103");
        assertThat(artist.get("artworkId").isNull()).isTrue();
        JsonNode album = document.get("albumDiscoveries").get(0).get("album");
        assertThat(album.get("name").asString()).isEqualTo("Album 1");
        assertThat(album.get("year").intValue()).isEqualTo(2020);
        assertThat(album.get("artistId").asString()).isEqualTo(id(1));
    }

    @Test
    void shouldPreserveLargePromptsAndResponsesAcrossTaskPages() {
        insertArtist(1, "Artist");
        insertAlbum(1, 1);
        insertJob(1);
        String rawRequest = "[{\"role\":\"system\",\"content\":\"" + "Full prompt ".repeat(8192) + "\"}]";
        String rawResult = "[{\"content\":\"" + "Full response ".repeat(8192) + "\"}]";
        for (Kind kind : Kind.values()) {
            insertDiscovery(kind, 1, 1, 1, CREATED);
            for (int index = 0; index < 19; index++) {
                int taskId = (kind == Kind.ARTIST ? 100 : 200) + index;
                insertTask(kind, taskId, 1, 1, "COMPLETE", "{}");
                jdbcTemplate.update("UPDATE discovery_task SET raw_request = ?, raw_result = ? WHERE id = ?",
                        rawRequest, rawResult, id(taskId));
            }
        }

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        JsonNode document = jsonMapper.readTree(download(apiTemplate.createCookieRequest(authentication.getStaticToken())).getBody());

        assertThat(document.get("complete").booleanValue()).isTrue();
        for (Kind kind : Kind.values()) {
            JsonNode tasks = document.get(kind.entityName() + "Discoveries").get(0).get("tasks");
            assertThat(tasks.size()).isEqualTo(19);
            for (JsonNode task : tasks) {
                assertThat(task.get("rawRequest").asString()).isEqualTo(rawRequest);
                assertThat(task.get("rawResult").asString()).isEqualTo(rawResult);
            }
        }
    }

    @Test
    void shouldExportMultipleDiscoveryPagesWithoutDuplicates() {
        insertJob(1);
        for (int index = 1; index <= 310; index++) {
            insertArtist(index, "Artist " + index);
            insertAlbum(index, index);
            if (index > 105) {
                for (Kind kind : Kind.values()) {
                    insertDiscovery(kind, index, index, 1, CREATED);
                }
            }
        }

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        JsonNode document = jsonMapper.readTree(download(apiTemplate.createHeaderRequest(authentication.getAccessToken())).getBody());

        for (Kind kind : Kind.values()) {
            JsonNode discoveries = document.get(kind.entityName() + "Discoveries");
            assertThat(discoveries.size()).isEqualTo(205);
            for (int index = 0; index < 205; index++) {
                assertThat(discoveries.get(index).get("id").asString()).isEqualTo(id(index + 106));
            }
        }
    }

    @Test
    void shouldStreamHeaderBeforeLoadingExportDataEvenWithTraceLoggingEnabled() {
        CountDownLatch firstBytesReceived = new CountDownLatch(1);
        doAnswer(invocation -> {
            assertThat(firstBytesReceived.await(10, TimeUnit.SECONDS)).isTrue();
            return List.of();
        }).when(artistRepository).findForEvaluation(eq(""), any());
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        try {
            apiTemplate.getRestTemplate().execute(PATH, HttpMethod.GET,
                    request -> request.getHeaders().addAll(apiTemplate.createCookieRequest(authentication.getStaticToken()).getHeaders()),
                    response -> {
                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                        try (JsonParser parser = jsonMapper.createParser(response.getBody())) {
                            assertThat(parser.nextToken()).isEqualTo(JsonToken.START_OBJECT);
                            firstBytesReceived.countDown();
                            boolean complete = false;
                            while (parser.nextToken() != null) {
                                if (parser.currentToken() == JsonToken.PROPERTY_NAME && "complete".equals(parser.currentName())) {
                                    complete = parser.nextToken() == JsonToken.VALUE_TRUE;
                                }
                            }
                            assertThat(complete).isTrue();
                        }
                        return null;
                    });
        } finally {
            firstBytesReceived.countDown();
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

    private void insertJob(int number) {
        jdbcTemplate.update("INSERT INTO discovery_job (id, creation_date, discovery_type, status) VALUES (?, ?, 'FULL', 'COMPLETE')",
                id(number), CREATED);
    }

    private void insertDiscovery(Kind kind, int number, int entity, int job, LocalDateTime creationDate) {
        jdbcTemplate.update("INSERT INTO %s_discovery (id, creation_date, %s_id, discovery_job_id) VALUES (?, ?, ?, ?)"
                        .formatted(kind.entityName(), kind.entityName()), id(number), creationDate, id(entity), id(job));
    }

    private void insertTask(Kind kind, int number, int discovery, int job, String status, String result) {
        jdbcTemplate.update("""
                INSERT INTO discovery_task (id, creation_date, update_date, discovery_job_id, type, status,
                                            parameter, result, raw_request, raw_result)
                VALUES (?, ?, ?, ?, 'SPOTIFY_ARTIST_DATA', ?, ?, ?, ?, ?)
                """, id(number), CREATED, CREATED.plusMinutes(1), id(job), status,
                "{\"artistId\":\"" + id(1) + "\"}", result, RAW_REQUEST, RAW_RESULT);
        jdbcTemplate.update("INSERT INTO %s_discovery_task (%s_discovery_id, discovery_task_id) VALUES (?, ?)"
                        .formatted(kind.entityName(), kind.entityName()), id(discovery), id(number));
    }

    private static String id(int number) {
        return "00000000-0000-0000-0000-%012d".formatted(number);
    }
}
