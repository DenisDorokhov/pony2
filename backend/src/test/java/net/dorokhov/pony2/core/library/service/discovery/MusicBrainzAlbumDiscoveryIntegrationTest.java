package net.dorokhov.pony2.core.library.service.discovery;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.hash.Hashing;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.observation.ObservationRegistry;
import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.config.service.command.ConfigSetUpdateCommand;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.AlbumDiscovery;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;
import net.dorokhov.pony2.api.library.domain.MusicBrainzAlbumData;
import net.dorokhov.pony2.api.llm.domain.LlmCache;
import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.installation.service.BuildVersionProvider;
import net.dorokhov.pony2.core.library.repository.AlbumDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryJobRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.discovery.musicbrainz.MusicBrainzClient;
import net.dorokhov.pony2.core.library.service.discovery.musicbrainz.MusicBrainzDto;
import net.dorokhov.pony2.core.library.service.discovery.task.MusicBrainzAlbumDataService;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import net.dorokhov.pony2.core.llm.repository.LlmCacheRepository;
import net.dorokhov.pony2.core.llm.service.ChatModelImpl;
import net.dorokhov.pony2.core.log.repository.LogMessageRepository;
import net.dorokhov.pony2.core.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.dorokhov.pony2.api.llm.domain.LlmCacheRegion.MUSICBRAINZ;
import static net.dorokhov.pony2.test.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MusicBrainzAlbumDiscoveryIntegrationTest extends IntegrationTest {

    private static final String SEARCH_URL = "https://musicbrainz.org/ws/2/release-group?query=test&fmt=json";
    private static final String ARTIST_URL = "https://musicbrainz.org/ws/2/artist/artist?fmt=json";

    @Autowired private AlbumDiscoveryService albumService;
    @Autowired private ArtistDiscoveryService artistService;
    @Autowired private LibraryJobSynchronizer jobSynchronizer;
    @Autowired private ConfigService configService;
    @Autowired private BuildVersionProvider buildVersionProvider;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private DiscoveryJobRepository jobRepository;
    @Autowired private DiscoveryTaskRepository taskRepository;
    @Autowired private ArtistDiscoveryRepository artistDiscoveryRepository;
    @Autowired private AlbumDiscoveryRepository albumDiscoveryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private LlmCacheRepository cacheRepository;
    @Autowired private LogMessageRepository logRepository;

    private MusicBrainzClient.Session session;
    private MusicBrainzClient.ExchangeObserver exchangeObserver;
    private LibraryJobSynchronizer.LibraryJobRegistration registration;
    private Artist artist;
    private Album album;
    private List<MusicBrainzDto.ReleaseGroup> groups;
    private String rawSearchResponse;
    private String rawArtistResponse;

    @BeforeEach
    void setUp() throws Exception {
        registration = jobSynchronizer.registerDiscoveryJob();
        configService.update(new ConfigSetUpdateCommand());
        userRepository.save(user().setEmail("ordinary@example.com").setRoles(Set.of(User.Role.USER)));
        User firstAdmin = userRepository.save(user().setEmail("first@example.com").setRoles(Set.of(User.Role.ADMIN)));
        User laterAdmin = userRepository.save(user().setEmail("later@example.com").setRoles(Set.of(User.Role.ADMIN)));
        jdbcTemplate.update("UPDATE pony_user SET creation_date = ? WHERE id = ?", LocalDateTime.of(2020, 1, 1, 0, 0), firstAdmin.getId());
        jdbcTemplate.update("UPDATE pony_user SET creation_date = ? WHERE id = ?", LocalDateTime.of(2021, 1, 1, 0, 0), laterAdmin.getId());
        artist = artistRepository.save(new Artist().setName("Slayer"));
        album = albumRepository.save(new Album().setArtist(artist).setName("Reign in Blood").setYear(1986));
        session = mock(MusicBrainzClient.Session.class);
        lenient().when(musicBrainzClient.openSession(anyString(), any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            exchangeObserver = invocation.getArgument(1);
            return session;
        });
        search(List.of(group("group", "Different group title", "reign in blood", List.of(credit("artist", "Slayer", "Slayer")))));
        rawArtistResponse = "{\"id\":\"artist\",\"area\":{\"iso-3166-1-codes\":[\"US\",\"GB\"]}}";
        lenient().when(session.getArtist(anyString())).thenAnswer(invocation -> {
            exchangeObserver.onRequest(ARTIST_URL);
            exchangeObserver.onResponse(ARTIST_URL, rawArtistResponse);
            return JsonConverter.fromJson(rawArtistResponse, MusicBrainzDto.Artist.class);
        });
        lenient().when(model.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
    }

    @AfterEach
    void tearDown() {
        registration.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCompleteExactReleaseMatchWithoutLlmAndAssociateOnlyWithAlbum(boolean cacheEnabled) {
        DiscoveryJob job = job(DiscoveryType.ALBUM);
        List<DiscoveryProgress> progress = new ArrayList<>();

        albumService.discover(job, album, cacheEnabled, value -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(taskRepository.countByJobIdAndStatus(job.getId(), DiscoveryTask.Status.COMPLETE))
                    .isEqualTo(value.getValue().getItemsComplete());
            progress.add(value);
        });

        assertThat(progress).allSatisfy(value -> assertThat(value.getStep()).isEqualTo(DiscoveryProgress.Step.ALBUM_DISCOVERY));
        assertThat(progress).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 1), DiscoveryProgress.Value.of(1, 1));
        DiscoveryTask task = onlyTask();
        assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(task.getParameter()).isEqualTo(JsonConverter.toJson(new DiscoveryTask.AlbumParameter(album.getId())));
        assertThat(JsonConverter.fromJson(task.getResult(), MusicBrainzAlbumData.class)).isEqualTo(
                new MusicBrainzAlbumData("group", "Album", List.of("Live", "Compilation"), "1986-10", "artist", List.of("US", "GB")));
        assertThat(JsonConverter.fromJson(task.getRawRequest(), Object[].class)).containsExactly(SEARCH_URL, ARTIST_URL);
        assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(rawSearchResponse, rawArtistResponse);
        assertThat(artistDiscoveryRepository.count()).isZero();
        getTransactionTemplate().executeWithoutResult(status -> {
            AlbumDiscovery discovery = albumDiscoveryRepository.findFirstByAlbumIdOrderByCreationDateDesc(album.getId()).orElseThrow();
            assertThat(discovery.getJob().getId()).isEqualTo(job.getId());
            assertThat(discovery.getTasks()).singleElement().satisfies(value -> assertThat(value.getId()).isEqualTo(task.getId()));
        });
        verify(musicBrainzClient).openSession(eq("pony2/" + buildVersionProvider.getBuildVersion().getVersion() + " (first@example.com)"), any());
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void shouldCountMultipleMatchingReleasesInTheSameGroupAsOneCandidate() {
        MusicBrainzDto.ReleaseGroup original = groups.getFirst();
        search(List.of(new MusicBrainzDto.ReleaseGroup(original.id(), original.title(), original.primaryType(), original.secondaryTypes(),
                original.firstReleaseDate(), original.artistCredit(), List.of(
                new MusicBrainzDto.Release("one", "Reign in Blood"), new MusicBrainzDto.Release("two", "REIGN IN BLOOD")))));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(onlyTask().getResult()).isNotEqualTo("null");
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void shouldNotAcceptMatchingGroupTitleOrAmbiguousReleaseTitlesWithoutLlm() {
        search(List.of(group("group", "Reign in Blood", "Different release", List.of(credit("artist", "Slayer", null)))));
        albumService.discover(job(DiscoveryType.ALBUM), album, null);
        assertThat(onlyTask().getResult()).isEqualTo("null");

        search(List.of(groups.getFirst(), group("second", "Other", "REIGN IN BLOOD", List.of(credit("artist", "Slayer", null))),
                group("third", "Other", "Reign in Blood", List.of(credit("artist", "Slayer", null)))));
        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(taskRepository.findAll()).allSatisfy(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(task.getResult()).isEqualTo("null");
            assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).hasSize(1);
        });
        verify(session, never()).getArtist(anyString());
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void shouldChooseArtistByCanonicalOrCreditedNameWithoutLlm() {
        search(List.of(group("group", "Other", album.getName(), List.of(
                credit("wrong", "Another Artist", "Another Artist"), credit("artist", "Different canonical name", "SLAYER")))));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(onlyTask().getResult()).isNotEqualTo("null");
        verify(session).getArtist("artist");
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void shouldReturnNullForUnmatchedArtistWithoutLlm() {
        search(List.of(group("group", "Other", album.getName(), List.of(credit("one", "A", null), credit("two", "B", null)))));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(onlyTask().getResult()).isEqualTo("null");
        verify(session, never()).getArtist(anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSelectWithLlmAndPersistMixedExchangesAndRespectCache(boolean cacheEnabled) {
        enableLlm();
        search(List.of(group("group", "Other", "Reign in Blood (deluxe)", List.of(credit("artist", "Slayer", null)))));
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(prompt.getInstructions().get(1).getText()).contains("complete-response-marker", "Reign in Blood (deluxe)");
            assertThat(((OpenAiChatOptions) prompt.getOptions()).getToolChoice()).isEqualTo("none");
            DiscoveryTask task = taskRepository.findAll().stream().filter(value -> value.getStatus() == DiscoveryTask.Status.STARTED).findFirst().orElseThrow();
            assertThat(JsonConverter.fromJson(task.getRawRequest(), Object[].class)).hasSize(2);
            assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(rawSearchResponse, null);
            return response("\"group\"");
        });

        albumService.discover(job(DiscoveryType.ALBUM), album, cacheEnabled, null);
        albumService.discover(job(DiscoveryType.ALBUM), album, cacheEnabled, null);

        assertThat(taskRepository.findAll()).allSatisfy(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(task.getResult()).isNotEqualTo("null");
            Object[] requests = JsonConverter.fromJson(task.getRawRequest(), Object[].class);
            assertThat(requests).hasSize(3);
            assertThat(requests[0]).isEqualTo(SEARCH_URL);
            assertThat(requests[1]).isInstanceOf(Map.class);
            assertThat(requests[2]).isEqualTo(ARTIST_URL);
            assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(rawSearchResponse, "\"group\"", rawArtistResponse);
        });
        verify(model, times(cacheEnabled ? 1 : 2)).call(any(Prompt.class));
        assertThat(cacheRepository.count()).isEqualTo(cacheEnabled ? 1 : 0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldUseConfiguredModelForMusicBrainzSelectionAndRetry(boolean artistSelection) throws IOException {
        String configuredModel = "qwen3.8-27b";
        ConcurrentLinkedQueue<String> requests = new ConcurrentLinkedQueue<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            try (exchange) {
                requests.add(new String(exchange.getRequestBody().readAllBytes(), UTF_8));
                String content = requests.size() == 1 ? "not JSON" : artistSelection ? "\"artist\"" : "\"group\"";
                byte[] body = """
                        {"id":"test","object":"chat.completion","created":1,"model":"%s",
                         "choices":[{"index":0,"message":{"role":"assistant","content":%s},"finish_reason":"stop"}]}
                        """.formatted(configuredModel, JsonConverter.toJson(content)).getBytes(UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
        });
        server.start();
        try {
            configService.update(new ConfigSetUpdateCommand()
                    .setLlmUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1")
                    .setLlmModel(configuredModel));
            ChatModelImpl httpModel = new ChatModelImpl(configService, ObservationRegistry.NOOP);
            when(model.createChatModel(any())).thenAnswer(invocation -> httpModel.createChatModel(invocation.getArgument(0)));
            search(List.of(group("group", "Other", artistSelection ? album.getName() : "Deluxe title",
                    artistSelection ? List.of(credit("artist", "A", null), credit("other", "B", null))
                            : List.of(credit("artist", "Slayer", null)))));

            albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

            assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(requests).hasSize(2);
            for (String body : requests) {
                Map<String, Object> request = JsonConverter.mapFromJson(body, String.class, Object.class);
                assertThat(request.get("model")).isEqualTo(configuredModel);
                assertThat(request.get("tool_choice")).isEqualTo("none");
            }
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldRespectCacheRegardlessOfJsonObjectFieldOrder(boolean cacheEnabled) {
        enableLlm();
        search(List.of(group("group", "Other", album.getName(), List.of(credit("artist", "A", null), credit("other", "B", null))),
                group("second", "Other", album.getName(), List.of(credit("second-artist", "C", null)))));
        when(model.call(any(Prompt.class))).thenReturn(response("\"group\""), response("\"artist\""),
                response("\"group\""), response("\"artist\""));
        String originalResponse = rawSearchResponse;

        albumService.discover(job(DiscoveryType.ALBUM), album, cacheEnabled, null);
        rawSearchResponse = JsonConverter.toPrettyJson(reverseObjectFields(JsonConverter.fromJson(originalResponse)));
        assertThat(rawSearchResponse).isNotEqualTo(originalResponse);
        albumService.discover(job(DiscoveryType.ALBUM), album, cacheEnabled, null);

        List<DiscoveryTask> tasks = taskRepository.findAll();
        assertThat(tasks).hasSize(2).allSatisfy(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(JsonConverter.fromJson(task.getResult(), MusicBrainzAlbumData.class).artistId()).isEqualTo("artist");
            Object[] requests = JsonConverter.fromJson(task.getRawRequest(), Object[].class);
            assertThat(requests).hasSize(4);
            assertThat(JsonConverter.toJson(requests[1])).startsWith("{\"systemPrompt\":");
            assertThat(((Map<?, ?>) requests[1]).get("userPrompt")).asString().contains(
                    "Album from the library: {\"title\":\"Reign in Blood\",\"year\":1986,\"artistName\":\"Slayer\"}");
            assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).hasSize(4);
        });
        assertThat(tasks.stream().map(task -> JsonConverter.fromJson(task.getRawResult(), String[].class)[0]))
                .containsExactlyInAnyOrder(originalResponse, rawSearchResponse);
        verify(model, times(cacheEnabled ? 2 : 4)).call(any(Prompt.class));
        if (cacheEnabled) {
            Object[] requests = JsonConverter.fromJson(tasks.getFirst().getRawRequest(), Object[].class);
            assertThat(cacheRepository.findAll()).allSatisfy(cache -> assertThat(cache.getRegion()).isEqualTo(MUSICBRAINZ))
                    .extracting(LlmCache::getKey).containsExactlyInAnyOrder(
                            "MUSICBRAINZ_ALBUM_DATA_RELEASE_GROUP:" + Hashing.sha256().hashString(JsonConverter.toJson(requests[1]), UTF_8),
                            "MUSICBRAINZ_ALBUM_DATA_ARTIST:" + Hashing.sha256().hashString(JsonConverter.toJson(requests[2]), UTF_8));
        } else {
            assertThat(cacheRepository.count()).isZero();
        }
    }

    @Test
    void shouldNotReuseCacheForDifferentAlbums() {
        enableLlm();
        search(List.of(group("group", "Other", "Deluxe title", List.of(credit("artist", "Slayer", null)))));
        when(model.call(any(Prompt.class))).thenReturn(response("\"group\""));
        Album otherAlbum = albumRepository.save(new Album().setArtist(artist).setName("South of Heaven").setYear(1988));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);
        albumService.discover(job(DiscoveryType.ALBUM), otherAlbum, null);

        verify(model, times(2)).call(any(Prompt.class));
        assertThat(cacheRepository.findAll()).hasSize(2).extracting(LlmCache::getKey).doesNotHaveDuplicates();
        assertThat(taskRepository.findAll()).allSatisfy(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldRespectCacheWhenCreatedChangesAndPreserveCompleteResponses(boolean cacheEnabled) {
        enableLlm();
        search(List.of(group("group", "Other", album.getName(), List.of(credit("artist", "A", null), credit("other", "B", null))),
                group("second", "Other", album.getName(), List.of(credit("second-artist", "C", null)))));
        Map<String, Object> searchResponse = JsonConverter.mapFromJson(rawSearchResponse, String.class, Object.class);
        searchResponse.put("created", "2026-10-11T10:00:00Z");
        searchResponse.put("metadata", Map.of("created", "album-metadata"));
        rawSearchResponse = JsonConverter.toJson(searchResponse);
        String originalResponse = rawSearchResponse;
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            String currentCreated = JsonConverter.mapFromJson(rawSearchResponse, String.class, Object.class).get("created").toString();
            assertThat(prompt.getInstructions().get(1).getText()).contains(
                    "\"created\":\"" + currentCreated + "\"", "\"metadata\":{\"created\":\"album-metadata\"}", "complete-response-marker");
            return response(prompt.getInstructions().get(1).getText().contains("Selected release group ID:") ? "\"artist\"" : "\"group\"");
        });

        albumService.discover(job(DiscoveryType.ALBUM), album, cacheEnabled, null);
        searchResponse.put("created", "2026-10-11T11:00:00Z");
        rawSearchResponse = JsonConverter.toPrettyJson(reverseObjectFields(searchResponse));
        albumService.discover(job(DiscoveryType.ALBUM), album, cacheEnabled, null);

        List<DiscoveryTask> tasks = taskRepository.findAll();
        assertThat(tasks).hasSize(2).allSatisfy(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(JsonConverter.fromJson(task.getResult(), MusicBrainzAlbumData.class).artistId()).isEqualTo("artist");
            Object[] requests = JsonConverter.fromJson(task.getRawRequest(), Object[].class);
            assertThat(requests).hasSize(4);
            for (int index : List.of(1, 2)) {
                Map<?, ?> request = (Map<?, ?>) requests[index];
                assertThat(request).hasSize(2);
                assertThat(request.get("userPrompt")).asString().contains(
                        "\"created\":\"2026-10-11T", "\"metadata\":{\"created\":\"album-metadata\"}", "complete-response-marker");
            }
        });
        assertThat(tasks.stream().map(task -> JsonConverter.fromJson(task.getRawResult(), String[].class)[0]))
                .containsExactlyInAnyOrder(originalResponse, rawSearchResponse);
        verify(model, times(cacheEnabled ? 2 : 4)).call(any(Prompt.class));
        assertThat(cacheRepository.count()).isEqualTo(cacheEnabled ? 2 : 0);
    }

    @Test
    void shouldNotReuseCacheWhenMusicBrainzDataChanges() {
        enableLlm();
        search(List.of(group("group", "Other", "Deluxe title", List.of(credit("artist", "Slayer", null)))));
        when(model.call(any(Prompt.class))).thenReturn(response("\"group\""));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);
        MusicBrainzDto.ReleaseGroup original = groups.getFirst();
        search(List.of(new MusicBrainzDto.ReleaseGroup(original.id(), original.title(), original.primaryType(), original.secondaryTypes(),
                "1987", original.artistCredit(), original.releases())));
        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        verify(model, times(2)).call(any(Prompt.class));
        assertThat(cacheRepository.findAll()).hasSize(2).extracting(LlmCache::getKey).doesNotHaveDuplicates();
        assertThat(taskRepository.findAll()).allSatisfy(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE))
                .extracting(task -> JsonConverter.fromJson(task.getResult(), MusicBrainzAlbumData.class).firstReleaseDate())
                .containsExactlyInAnyOrder("1986-10", "1987");
    }

    @ParameterizedTest
    @CsvSource({"false, not JSON", "false, '\"invented\"'", "true, not JSON", "true, '\"invented\"'"})
    void shouldPersistWarningAfterSuccessfulLlmRetry(boolean artistSelection, String rejectedResponse) {
        enableLlm();
        search(List.of(group("group", "Other", artistSelection ? album.getName() : "Deluxe title",
                artistSelection ? List.of(credit("artist", "A", null), credit("other", "B", null)) : List.of(credit("artist", "Slayer", null)))));
        when(model.call(any(Prompt.class))).thenReturn(response(rejectedResponse), response(artistSelection ? "\"artist\"" : "\"group\""));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(logRepository.findAll()).filteredOn(log -> log.getText().startsWith("Invalid MusicBrainz response from the LLM"))
                .singleElement().satisfies(log -> {
                    assertThat(log.getLevel()).isEqualTo(LogMessage.Level.WARN);
                    assertThat(log.getText()).contains(album.getId(), artistSelection ? "MUSICBRAINZ_ALBUM_DATA_ARTIST" : "MUSICBRAINZ_ALBUM_DATA_RELEASE_GROUP");
                    assertThat(log.getArguments().getLast()).isNotBlank();
                });
        verify(model, times(2)).call(any(Prompt.class));
    }

    @Test
    void shouldLogCompletedMusicBrainzResultAtDebugLevel() {
        Logger logger = (Logger) LoggerFactory.getLogger(MusicBrainzAlbumDataService.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try {
            albumService.discover(job(DiscoveryType.ALBUM), album, null);

            MusicBrainzAlbumData result = JsonConverter.fromJson(onlyTask().getResult(), MusicBrainzAlbumData.class);
            assertThat(appender.list).filteredOn(event -> event.getMessage().startsWith("MusicBrainz discovery result for album"))
                    .singleElement().satisfies(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
                        assertThat(event.getArgumentArray()).containsExactly(album.getId(), result);
                    });
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }

    @Test
    void shouldAskLlmForBothReleaseGroupAndArtistAndValidateReturnedIds() {
        enableLlm();
        search(List.of(group("group", "Other", album.getName(), List.of(credit("artist", "A", null), credit("other", "B", null))),
                group("second", "Other", album.getName(), List.of(credit("second-artist", "C", null)))));
        when(model.call(any(Prompt.class))).thenReturn(response("\"invented\""), response("\"group\""), response("\"artist\""));

        albumService.discover(job(DiscoveryType.ALBUM), album, true, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(JsonConverter.fromJson(onlyTask().getRawResult(), String[].class)).containsExactly(
                rawSearchResponse, "\"invented\"", "\"group\"", "\"artist\"", rawArtistResponse);
        verify(session).getArtist("artist");
        assertThat(cacheRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldReturnWholeResultNullWhenLlmCannotChooseArtist() {
        enableLlm();
        search(List.of(group("group", "Other", album.getName(), List.of(credit("one", "A", null), credit("two", "B", null)))));
        when(model.call(any(Prompt.class))).thenReturn(response("null"));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(onlyTask().getResult()).isEqualTo("null");
        verify(session, never()).getArtist(anyString());
    }

    @Test
    void shouldFailInvalidLlmResponseAfterOneVerificationAttempt() {
        enableLlm();
        search(List.of(group("group", "Other", "Deluxe title", List.of(credit("artist", "Slayer", null)))));
        when(model.call(any(Prompt.class))).thenReturn(response("not JSON"));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
        assertThat(JsonConverter.fromJson(onlyTask().getRawResult(), String[].class)).containsExactly(rawSearchResponse, "not JSON", "not JSON");
        assertThat(cacheRepository.count()).isZero();
        verify(model, times(2)).call(any(Prompt.class));
    }

    @Test
    void shouldPreserveRequestsWhenLlmFails() {
        enableLlm();
        search(List.of(group("group", "Other", "Deluxe title", List.of(credit("artist", "Slayer", null)))));
        when(model.call(any(Prompt.class))).thenThrow(new IllegalStateException("LLM unavailable"));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
        assertThat(onlyTask().getResult()).contains("LLM unavailable");
        assertThat(JsonConverter.fromJson(onlyTask().getRawResult(), String[].class)).containsExactly(rawSearchResponse, null);
    }

    @Test
    void shouldCompleteEmptySearchAndItemLimitWithNull() {
        search(List.of());
        albumService.discover(job(DiscoveryType.ALBUM), album, null);
        doAnswer(invocation -> {
            exchangeObserver.onRequest(SEARCH_URL);
            exchangeObserver.onResponse(SEARCH_URL, "{\"count\":1001}");
            throw new MusicBrainzClient.ItemLimitException(1000);
        }).when(session).searchReleaseGroups(anyString());
        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(taskRepository.findAll()).allSatisfy(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(task.getResult()).isEqualTo("null");
        });
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void shouldUseEmptyCountryArrayForMissingArea() {
        rawArtistResponse = "{\"id\":\"artist\"}";
        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(JsonConverter.fromJson(onlyTask().getResult(), MusicBrainzAlbumData.class).country()).isEmpty();
    }

    @Test
    void shouldSkipMissingNamesAndEscapeLuceneSpecialCharacters() {
        albumRepository.save(album.setName(null));
        albumService.discover(job(DiscoveryType.ALBUM), album, null);
        assertThat(onlyTask().getResult()).isEqualTo("null");
        verifyNoInteractions(musicBrainzClient);
        albumRepository.save(album.setName("A \"B\" \\ C/D"));
        search(List.of());

        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        verify(session).searchReleaseGroups("release:\"A \\\"B\\\" \\\\ C\\/D\" AND artistname:\"Slayer\"");
    }

    @Test
    void shouldFailApiErrorsAndInterruptCancellation() {
        doAnswer(invocation -> {
            exchangeObserver.onRequest(SEARCH_URL);
            throw new IllegalStateException("MusicBrainz unavailable");
        }).when(session).searchReleaseGroups(anyString());
        albumService.discover(job(DiscoveryType.ALBUM), album, null);
        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
        assertThat(onlyTask().getRawResult()).isEqualTo("[null]");
        doAnswer(invocation -> {
            jobSynchronizer.cancelDiscovery();
            throw new MusicBrainzClient.CancelledException();
        }).when(session).searchReleaseGroups(anyString());
        List<DiscoveryProgress> progress = new ArrayList<>();

        assertThatThrownBy(() -> albumService.discover(job(DiscoveryType.ALBUM), album, true, progress::add))
                .isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(progress).extracting(DiscoveryProgress::getValue).containsExactly(DiscoveryProgress.Value.of(0, 1));
        assertThat(taskRepository.findByStatus(DiscoveryTask.Status.INTERRUPTED)).hasSize(1);
        verify(session).cancel();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldDiscoverEveryAlbumAfterSpotifyAndKeepArtistProgress(boolean llmEnabled) {
        if (llmEnabled) {
            enableLlm();
            when(model.call(any(Prompt.class))).thenReturn(response("{\"status\":\"NOT_FOUND\"}"));
        }
        Album second = albumRepository.save(new Album().setArtist(artist).setName("Second album"));
        search(List.of());
        List<DiscoveryProgress> progress = new ArrayList<>();
        DiscoveryJob job = job(DiscoveryType.ARTIST);

        artistService.discover(job, artist, false, progress::add);

        int total = llmEnabled ? 4 : 2;
        assertThat(progress).hasSize(total + 1);
        for (int index = 0; index <= total; index++) {
            assertThat(progress.get(index).getStep()).isEqualTo(DiscoveryProgress.Step.ARTIST_DISCOVERY);
            assertThat(progress.get(index).getValue()).isEqualTo(DiscoveryProgress.Value.of(index, total));
        }
        List<DiscoveryTask> tasks = taskRepository.findAll().stream().sorted(Comparator.comparing(DiscoveryTask::getCreationDate)).toList();
        assertThat(tasks).hasSize(total).allSatisfy(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE));
        if (llmEnabled) {
            assertThat(tasks).extracting(DiscoveryTask::getType).containsExactly(DiscoveryTaskType.SPOTIFY_ARTIST_DATA,
                    DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA);
        }
        getTransactionTemplate().executeWithoutResult(status -> {
            assertThat(artistDiscoveryRepository.findAll()).singleElement().satisfies(discovery ->
                    assertThat(discovery.getTasks()).hasSize(llmEnabled ? 2 : 0));
            assertThat(albumDiscoveryRepository.findAll()).extracting(discovery -> discovery.getAlbum().getId())
                    .containsExactlyInAnyOrder(album.getId(), second.getId());
            assertThat(albumDiscoveryRepository.findAll()).allSatisfy(discovery ->
                    assertThat(discovery.getTasks()).singleElement().satisfies(task ->
                            assertThat(task.getType()).isEqualTo(DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA)));
        });
    }

    private void search(List<MusicBrainzDto.ReleaseGroup> candidates) {
        groups = candidates;
        String json = JsonConverter.toJson(new MusicBrainzDto.ReleaseGroupSearch(groups.size(), 0, groups));
        rawSearchResponse = json.substring(0, json.length() - 1) + ",\"extra\":\"complete-response-marker\"}";
        lenient().doAnswer(invocation -> {
            exchangeObserver.onRequest(SEARCH_URL);
            DiscoveryTask task = taskRepository.findAll().stream().filter(value -> value.getStatus() == DiscoveryTask.Status.STARTED).findFirst().orElseThrow();
            assertThat(task.getRawRequest()).contains(SEARCH_URL);
            assertThat(task.getRawResult()).isEqualTo("[null]");
            exchangeObserver.onResponse(SEARCH_URL, rawSearchResponse);
            return groups;
        }).when(session).searchReleaseGroups(anyString());
    }

    private Object reverseObjectFields(Object value) {
        if (value instanceof Map<?, ?> object) {
            Map<String, Object> reversed = new LinkedHashMap<>();
            object.keySet().stream().map(String.class::cast).sorted(Comparator.reverseOrder())
                    .forEach(key -> reversed.put(key, reverseObjectFields(object.get(key))));
            return reversed;
        }
        if (value instanceof List<?> array) {
            return array.stream().map(this::reverseObjectFields).toList();
        }
        return value;
    }

    private MusicBrainzDto.ReleaseGroup group(String id, String title, String releaseTitle, List<MusicBrainzDto.ArtistCredit> credits) {
        return new MusicBrainzDto.ReleaseGroup(id, title, "Album", List.of("Live", "Compilation"), "1986-10", credits,
                List.of(new MusicBrainzDto.Release("release-" + id, releaseTitle)));
    }

    private MusicBrainzDto.ArtistCredit credit(String id, String name, String creditedName) {
        return new MusicBrainzDto.ArtistCredit(creditedName, " / ", new MusicBrainzDto.Artist(id, name, null));
    }

    private DiscoveryJob job(DiscoveryType type) {
        return jobRepository.save(new DiscoveryJob().setType(type).setStatus(DiscoveryJob.Status.STARTED));
    }

    private DiscoveryTask onlyTask() {
        return taskRepository.findAll().getFirst();
    }

    private void enableLlm() {
        configService.update(new ConfigSetUpdateCommand().setLlmUrl("http://localhost:11434/v1").setLlmModel("test"));
    }

    private ChatResponse response(String value) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(value))));
    }
}
