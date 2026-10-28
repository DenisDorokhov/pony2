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
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscoveryIndex;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;
import net.dorokhov.pony2.api.library.domain.Genre;
import net.dorokhov.pony2.api.library.domain.MusicBrainzAlbumData;
import net.dorokhov.pony2.api.library.domain.Song;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.api.llm.domain.DiscoveryCache;
import net.dorokhov.pony2.api.llm.service.DiscoveryCacheService;
import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.installation.service.BuildVersionProvider;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryIndexRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryJobRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.repository.GenreRepository;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.discovery.musicbrainz.MusicBrainzClient;
import net.dorokhov.pony2.core.library.service.discovery.musicbrainz.MusicBrainzDto;
import net.dorokhov.pony2.core.library.service.discovery.task.MusicBrainzAlbumDataService;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import net.dorokhov.pony2.core.llm.repository.DiscoveryCacheRepository;
import net.dorokhov.pony2.core.llm.service.ChatModelImpl;
import net.dorokhov.pony2.core.log.repository.LogMessageRepository;
import net.dorokhov.pony2.core.user.repository.UserRepository;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.dorokhov.pony2.api.llm.domain.DiscoveryCacheRegion.MUSICBRAINZ;
import static net.dorokhov.pony2.test.SongFixtures.song;
import static net.dorokhov.pony2.test.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MusicBrainzAlbumDiscoveryIntegrationTest extends IntegrationTest {

    private static final String SEARCH_URL = "https://musicbrainz.org/ws/2/release-group?query=test&fmt=json";
    private static final String ARTIST_URL = "https://musicbrainz.org/ws/2/artist/artist?fmt=json";
    private static final String NORMALIZED_SEARCH_URL = "https://musicbrainz.org/ws/2/release-group?query=normalized&fmt=json";
    private static final String EMPTY_SEARCH_RESPONSE = "{\"count\":0,\"offset\":0,\"release-groups\":[]}";

    @Autowired private AlbumDiscoveryService albumService;
    @Autowired private DiscoveryTaskExecutor taskExecutor;
    @Autowired private MusicBrainzAlbumDataService musicBrainzAlbumDataService;
    @Autowired private ArtistDiscoveryService artistService;
    @Autowired private FullDiscoveryService fullDiscoveryService;
    @Autowired private LibraryJobSynchronizer jobSynchronizer;
    @Autowired private ConfigService configService;
    @Autowired private BuildVersionProvider buildVersionProvider;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private GenreRepository genreRepository;
    @Autowired private SongRepository songRepository;
    @Autowired private DiscoveryJobRepository jobRepository;
    @Autowired private DiscoveryTaskRepository taskRepository;
    @Autowired private ArtistDiscoveryIndexRepository artistDiscoveryIndexRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DiscoveryCacheRepository cacheRepository;
    @Autowired private DiscoveryCacheService cacheService;
    @Autowired private LogMessageRepository logRepository;

    @Value("${pony.llm.timeout:10m}")
    private Duration llmTimeout;

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
        lenient().when(musicBrainzClient.openSession(anyString(), any(), anyBoolean())).thenAnswer(invocation -> {
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
        notFoundResponses();
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
        assertThat(task.getAlbum().getId()).isEqualTo(album.getId());
        assertThat(JsonConverter.fromJson(task.getResult(), MusicBrainzAlbumData.class)).isEqualTo(
                new MusicBrainzAlbumData("group", "Album", List.of("Live", "Compilation"), "1986-10", "artist", List.of("US", "GB")));
        assertThat(JsonConverter.fromJson(task.getRawRequest(), Object[].class)).containsExactly(SEARCH_URL, ARTIST_URL);
        assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(rawSearchResponse, rawArtistResponse);
        assertThat(artistTasks()).isEmpty();
        getTransactionTemplate().executeWithoutResult(status -> {
            assertThat(taskRepository.findById(task.getId())).get().satisfies(value -> {
                assertThat(value.getJob().getId()).isEqualTo(job.getId());
                assertThat(value.getAlbum().getId()).isEqualTo(album.getId());
                assertThat(value.getArtist()).isNull();
            });
        });
        verify(musicBrainzClient).openSession(eq("pony2/" + buildVersionProvider.getBuildVersion().getVersion() + " (first@example.com)"), any(), eq(cacheEnabled));
        verify(model, never()).call(any(Prompt.class));
        verify(session, times(1)).searchReleaseGroups(anyString());
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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCacheHttpResponsesPreserveLlmContextAndRefreshExpiredEntries(boolean cacheEnabled) throws IOException {
        enableLlm();
        search(List.of(group("group", "Other", "Deluxe title", List.of(credit("artist", "Slayer", null)))));
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            assertThat(prompt.getInstructions().get(1).getText()).contains("complete-response-marker");
            return response("{\"id\":\"group\"}");
        });
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ws/2/", exchange -> {
            requests.incrementAndGet();
            String body = exchange.getRequestURI().getPath().equals("/ws/2/artist/artist") ? rawArtistResponse : rawSearchResponse;
            byte[] bytes = body.getBytes(UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/ws/2/";
        MusicBrainzClient client = new MusicBrainzClient(cacheService, baseUrl, Duration.ZERO, Duration.ZERO, 0,
                Duration.ofSeconds(10), 25);
        when(musicBrainzClient.openSession(anyString(), any(), anyBoolean())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return client.openSession(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2));
        });
        server.start();
        try {
            albumService.discover(job(DiscoveryType.ALBUM), album, cacheEnabled, null);
            albumService.discover(job(DiscoveryType.ALBUM), album, cacheEnabled, null);

            assertThat(requests).hasValue(cacheEnabled ? 2 : 4);
            assertThat(taskRepository.findAll()).hasSize(2).allSatisfy(task -> {
                assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
                Object[] rawRequests = JsonConverter.fromJson(task.getRawRequest(), Object[].class);
                assertThat(rawRequests).hasSize(3);
                assertThat(rawRequests[0]).asString().startsWith(baseUrl + "release-group?");
                assertThat(rawRequests[2]).isEqualTo(baseUrl + "artist/artist?fmt=json");
                assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class))
                        .containsExactly(rawSearchResponse, "{\"id\":\"group\"}", rawArtistResponse);
            });
            List<DiscoveryCache> httpEntries = cacheRepository.findAll().stream()
                    .filter(entry -> entry.getKey().startsWith("MUSICBRAINZ_HTTP:")).toList();
            assertThat(httpEntries).hasSize(cacheEnabled ? 2 : 0);
            assertThat(cacheRepository.count()).isEqualTo(cacheEnabled ? 3 : 0);
            if (cacheEnabled) {
                assertThat(httpEntries).allSatisfy(entry -> {
                    assertThat(entry.getRegion()).isEqualTo(MUSICBRAINZ);
                    assertThat(entry.getExpirationDate()).isEqualTo(entry.getCreationDate().plusSeconds(MUSICBRAINZ.getExpirationInSeconds()));
                }).extracting(DiscoveryCache::getValue).containsExactlyInAnyOrder(rawSearchResponse, rawArtistResponse);
                cacheRepository.saveAll(httpEntries.stream()
                        .map(entry -> entry.setExpirationDate(LocalDateTime.now().minusSeconds(1))).toList());

                albumService.discover(job(DiscoveryType.ALBUM), album, true, null);

                assertThat(requests).hasValue(4);
                assertThat(taskRepository.findAll()).hasSize(3).allSatisfy(task ->
                        assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE));
                assertThat(cacheRepository.count()).isEqualTo(3);
            }
            verify(model, times(cacheEnabled ? 1 : 2)).call(any(Prompt.class));
        } finally {
            client.onApplicationShutdown();
            server.stop(0);
        }
    }

    @Test
    void shouldAskLlmWhenOnlyGroupTitleMatchesOrReleaseTitlesAreAmbiguous() {
        when(model.call(any(Prompt.class))).thenReturn(response("{\"id\":null}"));
        search(List.of(group("group", "Reign in Blood", "Different release", List.of(credit("artist", "Slayer", null)))));
        albumService.discover(job(DiscoveryType.ALBUM), album, null);
        assertThat(onlyTask().getResult()).isEqualTo("null");

        search(List.of(groups.getFirst(), group("second", "Other", "REIGN IN BLOOD", List.of(credit("artist", "Slayer", null))),
                group("third", "Other", "Reign in Blood", List.of(credit("artist", "Slayer", null)))));
        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(taskRepository.findAll()).allSatisfy(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(task.getResult()).isEqualTo("null");
            assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).hasSize(2);
        });
        verify(session, never()).getArtist(anyString());
        verify(model, times(2)).call(any(Prompt.class));
    }

    @ParameterizedTest
    @CsvSource({"true,true", "false,true", "true,false", "false,false"})
    void shouldUseOnlyCurrentSuccessfulSearchAndReuseSelectionCacheAfterHttpRetries(boolean normalizedSearch, boolean cacheEnabled) throws IOException {
        enableLlm();
        if (normalizedSearch) {
            albumRepository.save(album.setName("Reign in Blood (CD1 of 2)"));
        }
        search(List.of(group("group", "Other", "Reign in Blood (deluxe)",
                List.of(credit("artist", "A", null), credit("other", "B", null)))));
        String normalization = "{\"searchTitle\":\"Reign in Blood\",\"searchFragments\":[],\"alternativeTitles\":[]}";
        String busy = "{\"error\":\"server-busy-marker\"}";
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            Prompt prompt = invocation.getArgument(0);
            if (prompt.getInstructions().getFirst().getText().startsWith("You normalize untrusted album-title text")) {
                return response(normalization);
            }
            String userPrompt = prompt.getInstructions().get(1).getText();
            assertThat(userPrompt).doesNotContain("server-busy-marker", EMPTY_SEARCH_RESPONSE, "Complete MusicBrainz search responses");
            String searchJson = userPrompt.split("\nMusicBrainz search response: ", 2)[1].split("\n", 2)[0];
            assertThat(JsonConverter.fromJson(searchJson)).isEqualTo(JsonConverter.fromJson(rawSearchResponse));
            return response(userPrompt.contains("\nSelected release group ID:") ? "{\"id\":\"artist\"}" : "{\"id\":\"group\"}");
        });
        AtomicInteger requests = new AtomicInteger();
        AtomicInteger candidateRequests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ws/2/", exchange -> {
            try (exchange) {
                requests.incrementAndGet();
                int status = 200;
                String body;
                if (exchange.getRequestURI().getPath().contains("/artist/")) {
                    body = rawArtistResponse;
                } else if (normalizedSearch && URLDecoder.decode(exchange.getRequestURI().getRawQuery(), UTF_8).contains("CD1")) {
                    body = EMPTY_SEARCH_RESPONSE;
                } else if (candidateRequests.incrementAndGet() <= 2) {
                    status = 503;
                    body = busy;
                } else {
                    body = rawSearchResponse;
                }
                byte[] bytes = body.getBytes(UTF_8);
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        MusicBrainzClient client = new MusicBrainzClient(cacheService,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/ws/2/", Duration.ZERO, Duration.ZERO, 2, Duration.ofSeconds(10), 25);
        when(musicBrainzClient.openSession(anyString(), any(), anyBoolean())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return client.openSession(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2));
        });
        server.start();
        try {
            albumService.discover(job(DiscoveryType.ALBUM), album, cacheEnabled, null);
            albumService.discover(job(DiscoveryType.ALBUM), album, cacheEnabled, null);

            List<DiscoveryTask> tasks = taskRepository.findAll().stream().sorted(Comparator.comparing(DiscoveryTask::getCreationDate)).toList();
            assertThat(tasks).hasSize(2).allSatisfy(task -> {
                assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
                assertThat(JsonConverter.fromJson(task.getResult(), MusicBrainzAlbumData.class).releaseGroupId()).isEqualTo("group");
                assertThat(task.getRawRequest()).doesNotContain("server-busy-marker");
            });
            List<String> expectedResponses = new ArrayList<>();
            if (normalizedSearch) {
                expectedResponses.add(EMPTY_SEARCH_RESPONSE);
                expectedResponses.add(normalization);
            }
            expectedResponses.addAll(List.of(busy, busy, rawSearchResponse, "{\"id\":\"group\"}", "{\"id\":\"artist\"}", rawArtistResponse));
            assertThat(JsonConverter.listFromJson(tasks.getFirst().getRawResult(), String.class)).containsExactlyElementsOf(expectedResponses);
            expectedResponses.removeAll(List.of(busy));
            assertThat(JsonConverter.listFromJson(tasks.getLast().getRawResult(), String.class)).containsExactlyElementsOf(expectedResponses);
            Object[] firstRequests = JsonConverter.fromJson(tasks.getFirst().getRawRequest(), Object[].class);
            Object[] secondRequests = JsonConverter.fromJson(tasks.getLast().getRawRequest(), Object[].class);
            int groupRequestIndex = normalizedSearch ? 5 : 3;
            assertThat(firstRequests[groupRequestIndex]).isEqualTo(secondRequests[groupRequestIndex - 2]);
            assertThat(firstRequests[groupRequestIndex + 1]).isEqualTo(secondRequests[groupRequestIndex - 1]);
            int callsPerDiscovery = normalizedSearch ? 3 : 2;
            verify(model, times(cacheEnabled ? callsPerDiscovery : 2 * callsPerDiscovery)).call(any(Prompt.class));
            assertThat(requests).hasValue((normalizedSearch ? 5 : 4) + (cacheEnabled ? 0 : normalizedSearch ? 3 : 2));
            assertThat(candidateRequests).hasValue(cacheEnabled ? 3 : 4);
            assertThat(cacheRepository.findAll()).hasSize(cacheEnabled ? normalizedSearch ? 6 : 4 : 0).allSatisfy(entry ->
                    assertThat(entry.getValue()).doesNotContain("server-busy-marker"));
        } finally {
            client.onApplicationShutdown();
            server.stop(0);
        }
    }

    @ParameterizedTest
    @CsvSource({"503,service unavailable", "200,not JSON", "200,'{\"error\":\"server busy\"}'"})
    void shouldFailInvalidSearchWithoutCallingLlm(int status, String response) throws IOException {
        enableLlm();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ws/2/", exchange -> {
            try (exchange) {
                byte[] bytes = response.getBytes(UTF_8);
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        MusicBrainzClient client = new MusicBrainzClient(cacheService,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/ws/2/", Duration.ZERO, Duration.ZERO, 0, Duration.ofSeconds(10), 25);
        when(musicBrainzClient.openSession(anyString(), any(), anyBoolean())).thenAnswer(invocation ->
                client.openSession(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
        server.start();
        try {
            albumService.discover(job(DiscoveryType.ALBUM), album, true, null);

            assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
            assertThat(JsonConverter.fromJson(onlyTask().getRawResult(), String[].class)).containsExactly(response);
            assertThat(cacheRepository.count()).isZero();
            verify(model, never()).call(any(Prompt.class));
        } finally {
            client.onApplicationShutdown();
            server.stop(0);
        }
    }

    @Test
    void shouldLogArtistAndAlbumNamesWhenDiscoveryFailsForDetachedAlbum() {
        Album detachedAlbum = albumRepository.findById(album.getId()).orElseThrow();
        assertThat(Hibernate.isInitialized(detachedAlbum.getArtist())).isFalse();
        doThrow(new IllegalStateException("MusicBrainz unavailable")).when(session).searchReleaseGroups(anyString());

        albumService.discover(job(DiscoveryType.ALBUM), detachedAlbum, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
        assertThat(logRepository.findAll())
                .filteredOn(message -> message.getLevel() == LogMessage.Level.ERROR)
                .extracting(LogMessage::getText)
                .containsExactly("Could not discover MusicBrainz data for album 'Slayer' -> 'Reign in Blood'.");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldManageTaskLifecycleTransactionsAndPreserveFailedStatus(boolean errorHookFails) {
        DiscoveryJob discoveryJob = job(DiscoveryType.ALBUM);
        IllegalStateException taskFailure = new IllegalStateException("Discovery failed");
        AtomicInteger errorHookCalls = new AtomicInteger();

        DiscoveryTaskExecutor.TaskResult<String> result = taskExecutor.execute(new DiscoveryTaskExecution<String>() {
            @Override
            public DiscoveryTask startTask() {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                return taskRepository.save(new DiscoveryTask()
                        .setJob(discoveryJob)
                        .setAlbum(album)
                        .setType(DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA)
                        .setStatus(DiscoveryTask.Status.STARTED));
            }

            @Override
            public String executeTask(DiscoveryTask task) {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                assertThat(taskRepository.findById(task.getId()).orElseThrow().getStatus())
                        .isEqualTo(DiscoveryTask.Status.STARTED);
                throw taskFailure;
            }

            @Override
            public void onError(DiscoveryTask task, Throwable error) {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                assertThat(error).isSameAs(taskFailure);
                Album taskAlbum = task.getAlbum();
                assertThat(taskAlbum).isNotNull();
                assertThat(taskAlbum.getArtist().getName()).isEqualTo("Slayer");
                DiscoveryTask.Status committedStatus = getTransactionTemplate().execute(status ->
                        taskRepository.findById(task.getId()).orElseThrow().getStatus());
                assertThat(committedStatus).isEqualTo(DiscoveryTask.Status.FAILED);
                errorHookCalls.incrementAndGet();
                if (errorHookFails) {
                    throw new IllegalStateException("Error hook failed");
                }
            }
        });

        assertThat(errorHookCalls).hasValue(1);
        assertThat(result.status()).isEqualTo(DiscoveryTask.Status.FAILED);
        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
        assertThat(onlyTask().getResult()).contains("Discovery failed").doesNotContain("Error hook failed");
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
    void shouldReturnNullWhenLlmCannotSelectUnmatchedArtist() {
        when(model.call(any(Prompt.class))).thenReturn(response("{\"id\":null}"));
        search(List.of(group("group", "Other", album.getName(), List.of(credit("one", "A", null), credit("two", "B", null)))));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(onlyTask().getResult()).isEqualTo("null");
        verify(session, never()).getArtist(anyString());
        verify(model).call(any(Prompt.class));
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
            return response("{\"id\":\"group\"}");
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
            assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(rawSearchResponse, "{\"id\":\"group\"}", rawArtistResponse);
        });
        verify(model, times(cacheEnabled ? 1 : 2)).call(any(Prompt.class));
        assertThat(cacheRepository.count()).isEqualTo(cacheEnabled ? 1 : 0);
    }

    @Test
    void shouldSupplyOrderedLibraryTracksForAmbiguousReleaseGroups() {
        enableLlm();
        Genre genre = genreRepository.save(new Genre().setName("Genre"));
        songRepository.save(song().setPath("second").setName("Second").setDiscNumber(1).setTrackNumber(2)
                .setDuration(240L).setArtistName("Second Artist").setAlbumArtistName("Various Artists").setAlbum(album).setGenre(genre));
        songRepository.save(song().setPath("third").setName("Third").setDiscNumber(2).setTrackNumber(1)
                .setDuration(300L).setArtistName("Third Artist").setAlbumArtistName("Various Artists").setAlbum(album).setGenre(genre));
        songRepository.save(song().setPath("first").setName("First").setDiscNumber(1).setTrackNumber(1)
                .setDuration(180L).setArtistName("First Artist").setAlbumArtistName("Various Artists").setAlbum(album).setGenre(genre));
        MusicBrainzDto.ReleaseGroup albumGroup = groups.getFirst();
        MusicBrainzDto.ReleaseGroup singleGroup = new MusicBrainzDto.ReleaseGroup("single", album.getName(), "Single", List.of(),
                "1986-10", albumGroup.artistCredit(), List.of(new MusicBrainzDto.Release("single-release", album.getName())));
        search(List.of(albumGroup, singleGroup));
        when(model.call(any(Prompt.class))).thenReturn(response("{\"id\":\"group\"}"));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(model, times(1)).call(captor.capture());
        Prompt selectionPrompt = captor.getAllValues().getFirst();
        assertThat(selectionPrompt.getInstructions().get(1).getText()).contains("Library album track count: 3");
        String trackJson = selectionPrompt.getInstructions().get(1).getText().split("\nLibrary album tracks: ", 2)[1];
        assertThat(JsonConverter.listFromJson(trackJson, Object.class)).containsExactly(
                Map.of("title", "First", "discNumber", 1, "trackNumber", 1, "durationSeconds", 180,
                        "artistName", "First Artist", "albumArtistName", "Various Artists"),
                Map.of("title", "Second", "discNumber", 1, "trackNumber", 2, "durationSeconds", 240,
                        "artistName", "Second Artist", "albumArtistName", "Various Artists"),
                Map.of("title", "Third", "discNumber", 2, "trackNumber", 1, "durationSeconds", 300,
                        "artistName", "Third Artist", "albumArtistName", "Various Artists"));
        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"title", "duration", "count", "artistName", "albumArtistName"})
    void shouldNotReuseCacheWhenLibraryTracksChange(String change) {
        enableLlm();
        Genre genre = genreRepository.save(new Genre().setName("Genre"));
        Song track = songRepository.save(song().setPath("first").setName("First").setAlbum(album).setGenre(genre));
        search(List.of(group("group", "Other", "Reign in Blood (deluxe)", List.of(credit("artist", "Slayer", null)))));
        when(model.call(any(Prompt.class))).thenReturn(response("{\"id\":\"group\"}"));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);
        albumService.discover(job(DiscoveryType.ALBUM), album, null);
        verify(model, times(1)).call(any(Prompt.class));

        switch (change) {
            case "title" -> songRepository.save(track.setName("Updated title"));
            case "duration" -> songRepository.save(track.setDuration(300L));
            case "count" -> songRepository.save(song().setPath("second").setName("Second").setAlbum(album).setGenre(genre));
            case "artistName" -> songRepository.save(track.setArtistName("Updated artist"));
            case "albumArtistName" -> songRepository.save(track.setAlbumArtistName("Updated album artist"));
            default -> throw new IllegalArgumentException(change);
        }
        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        verify(model, times(2)).call(any(Prompt.class));
        assertThat(cacheRepository.count()).isEqualTo(2);
        assertThat(taskRepository.findAll()).hasSize(3)
                .allSatisfy(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldUseConfiguredModelForMusicBrainzSelectionAndRetry(boolean artistSelection) throws IOException {
        String configuredModel = "qwen3.8-27b";
        ConcurrentLinkedQueue<String> requests = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> requestTimeouts = new ConcurrentLinkedQueue<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            try (exchange) {
                requests.add(new String(exchange.getRequestBody().readAllBytes(), UTF_8));
                requestTimeouts.add(exchange.getRequestHeaders().getFirst("X-Stainless-Timeout"));
                String content = requests.size() == 1 ? "not JSON" : artistSelection ? "{\"id\":\"artist\"}" : "{\"id\":\"group\"}";
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
                    .setDiscoveryEnabled(true).setLlmUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1")
                    .setLlmModel(configuredModel));
            ChatModelImpl httpModel = new ChatModelImpl(configService, ObservationRegistry.NOOP, llmTimeout);
            when(model.createChatModel(any())).thenAnswer(invocation -> httpModel.createChatModel(invocation.getArgument(0)));
            search(List.of(group("group", "Other", artistSelection ? album.getName() : "Deluxe title",
                    artistSelection ? List.of(credit("artist", "A", null), credit("other", "B", null))
                            : List.of(credit("artist", "Slayer", null)))));

            albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

            assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(requests).hasSize(2);
            String expectedTimeout = Long.toString(llmTimeout.toSeconds());
            assertThat(requestTimeouts).containsExactly(expectedTimeout, expectedTimeout);
            List<String> requestBodies = new ArrayList<>(requests);
            for (int index = 0; index < requestBodies.size(); index++) {
                Map<String, Object> request = JsonConverter.mapFromJson(requestBodies.get(index), String.class, Object.class);
                assertThat(request.get("model")).isEqualTo(configuredModel);
                assertThat(request.get("tool_choice")).isEqualTo("none");
                assertThat(request).doesNotContainKey("tools");
                Map<?, ?> systemMessage = (Map<?, ?>) ((List<?>) request.get("messages")).getFirst();
                String systemPrompt = systemMessage.get("content").toString();
                assertThat(systemPrompt).doesNotContain("```");
                Map<?, ?> responseFormat = (Map<?, ?>) request.get("response_format");
                assertThat(systemPrompt).doesNotContain("\"$schema\"");
                assertThat(responseFormat.get("type")).isEqualTo("json_schema");
                Map<?, ?> jsonSchema = (Map<?, ?>) responseFormat.get("json_schema");
                assertThat(jsonSchema.get("name")).isEqualTo("json_schema");
                assertThat(jsonSchema.get("strict")).isEqualTo(true);
                Map<?, ?> schema = (Map<?, ?>) jsonSchema.get("schema");
                List<?> messages = (List<?>) request.get("messages");
                Map<?, ?> schemaMessage = (Map<?, ?>) messages.getLast();
                String schemaPrompt = schemaMessage.get("content").toString();
                assertThat(schemaPrompt).contains("\n\n");
                assertThat(JsonConverter.fromJson(schemaPrompt.split("\n\n", 2)[1], Map.class)).isEqualTo(schema);
                assertThat(schema.get("type")).isEqualTo("object");
                assertThat(schema.get("required")).isEqualTo(List.of("id"));
                assertThat(schema.get("additionalProperties")).isEqualTo(false);
                Map<?, ?> id = (Map<?, ?>) ((Map<?, ?>) schema.get("properties")).get("id");
                assertThat(id.get("type")).isEqualTo(List.of("string", "null"));
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
        when(model.call(any(Prompt.class))).thenReturn(response("{\"id\":\"group\"}"),
                response("{\"id\":\"artist\"}"),
                response("{\"id\":\"group\"}"),
                response("{\"id\":\"artist\"}"));
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
                    .extracting(DiscoveryCache::getKey).containsExactlyInAnyOrder(
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
        when(model.call(any(Prompt.class))).thenReturn(response("{\"id\":\"group\"}"));
        Album otherAlbum = albumRepository.save(new Album().setArtist(artist).setName("South of Heaven").setYear(1988));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);
        albumService.discover(job(DiscoveryType.ALBUM), otherAlbum, null);

        verify(model, times(2)).call(any(Prompt.class));
        assertThat(cacheRepository.findAll()).hasSize(2).extracting(DiscoveryCache::getKey).doesNotHaveDuplicates();
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
            return response(prompt.getInstructions().get(1).getText().contains("Selected release group ID:") ? "{\"id\":\"artist\"}" : "{\"id\":\"group\"}");
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
                assertThat(request).hasSize(3);
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
        when(model.call(any(Prompt.class))).thenReturn(response("{\"id\":\"group\"}"));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);
        MusicBrainzDto.ReleaseGroup original = groups.getFirst();
        search(List.of(new MusicBrainzDto.ReleaseGroup(original.id(), original.title(), original.primaryType(), original.secondaryTypes(),
                "1987", original.artistCredit(), original.releases())));
        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        verify(model, times(2)).call(any(Prompt.class));
        assertThat(cacheRepository.findAll()).hasSize(2).extracting(DiscoveryCache::getKey).doesNotHaveDuplicates();
        assertThat(taskRepository.findAll()).allSatisfy(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE))
                .extracting(task -> JsonConverter.fromJson(task.getResult(), MusicBrainzAlbumData.class).firstReleaseDate())
                .containsExactlyInAnyOrder("1986-10", "1987");
    }

    @ParameterizedTest
    @CsvSource({"false, not JSON", "false, '{\"id\":\"invented\"}'", "false, '{}'", "false, '{\"id\":123}'",
            "false, 'null'", "false, '\"group\"'", "false, '{\"release-group-id\":\"group\"}'",
            "true, not JSON", "true, '{\"id\":\"invented\"}'", "true, '{}'", "true, '{\"id\":123}'",
            "true, 'null'", "true, '\"artist\"'", "true, '{\"artist-id\":\"artist\"}'"})
    void shouldPersistWarningAfterSuccessfulLlmRetry(boolean artistSelection, String rejectedResponse) {
        enableLlm();
        search(List.of(group("group", "Other", artistSelection ? album.getName() : "Deluxe title",
                artistSelection ? List.of(credit("artist", "A", null), credit("other", "B", null)) : List.of(credit("artist", "Slayer", null)))));
        when(model.call(any(Prompt.class))).thenReturn(response(rejectedResponse),
                response(artistSelection ? "{\"id\":\"artist\"}" : "{\"id\":\"group\"}"));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(model, times(2)).call(captor.capture());
        for (Prompt prompt : captor.getAllValues()) {
            assertThat(((OpenAiChatOptions) prompt.getOptions()).getResponseFormat().getType())
                    .isEqualTo(ResponseFormat.Type.JSON_SCHEMA);
            Map<?, ?> schema = JsonConverter.fromJson(((OpenAiChatOptions) prompt.getOptions()).getResponseFormat().getJsonSchema(), Map.class);
            assertThat(schema.get("type")).isEqualTo("object");
            assertThat(schema.get("required")).isEqualTo(List.of("id"));
            Map<?, ?> idSchema = (Map<?, ?>) ((Map<?, ?>) schema.get("properties")).get("id");
            assertThat(idSchema.get("type")).isEqualTo(List.of("string", "null"));
        }
        assertThat(captor.getAllValues().get(1).getInstructions())
                .anySatisfy(message -> assertThat(message.getText()).contains("JSON object", "Set id"));
        assertThat(logRepository.findAll()).filteredOn(log -> log.getText().startsWith("Invalid MusicBrainz response from the LLM"))
                .singleElement().satisfies(log -> {
                    assertThat(log.getLevel()).isEqualTo(LogMessage.Level.WARN);
                    assertThat(log.getText()).contains(artist.getName(), album.getName(),
                            artistSelection ? "artist selection" : "release group selection");
                    assertThat(log.getArguments().getLast()).contains("Exception", "\tat ");
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
        when(model.call(any(Prompt.class))).thenReturn(response("{\"id\":\"invented\"}"),
                response("{\"id\":\"group\"}"), response("{\"id\":\"artist\"}"));

        albumService.discover(job(DiscoveryType.ALBUM), album, true, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(JsonConverter.fromJson(onlyTask().getRawResult(), String[].class)).containsExactly(
                rawSearchResponse, "{\"id\":\"invented\"}", "{\"id\":\"group\"}", "{\"id\":\"artist\"}", rawArtistResponse);
        verify(session).getArtist("artist");
        assertThat(cacheRepository.count()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnWholeResultNullWhenLlmCannotChoose(boolean artistSelection) {
        enableLlm();
        search(List.of(group("group", "Other", artistSelection ? album.getName() : "Deluxe title",
                artistSelection ? List.of(credit("one", "A", null), credit("two", "B", null))
                        : List.of(credit("artist", "Slayer", null)))));
        when(model.call(any(Prompt.class))).thenReturn(response("{\"id\":null}"));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(onlyTask().getResult()).isEqualTo("null");
        verify(session, never()).getArtist(anyString());
    }

    @Test
    void shouldFailInvalidLlmResponseAfterThreeVerificationAttempts() {
        enableLlm();
        search(List.of(group("group", "Other", "Deluxe title", List.of(credit("artist", "Slayer", null)))));
        when(model.call(any(Prompt.class))).thenReturn(response("not JSON"));

        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
        assertThat(JsonConverter.fromJson(onlyTask().getRawResult(), String[].class))
                .containsExactly(rawSearchResponse, "not JSON", "not JSON", "not JSON", "not JSON");
        assertThat(cacheRepository.count()).isZero();
        verify(model, times(4)).call(any(Prompt.class));
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
    void shouldCompleteEmptySearchWithNullAfterNormalization() {
        notFoundResponses();
        search(List.of());
        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(taskRepository.findAll()).allSatisfy(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(task.getResult()).isEqualTo("null");
        });
        verify(model).call(any(Prompt.class));
        verify(session, times(1)).searchReleaseGroups(anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldNormalizeEmptySearchInOneCallAndPreserveHistoryAndCache(boolean cacheEnabled) throws IOException {
        enableLlm();
        searchAfterEmpty(groups);
        String normalization = "{\"searchTitle\":\"Reign in Blood\",\"searchFragments\":[\"Reign\"],\"alternativeTitles\":[]}";
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return response(normalization);
        });

        albumService.discover(job(DiscoveryType.ALBUM), album, cacheEnabled, null);
        albumService.discover(job(DiscoveryType.ALBUM), album, cacheEnabled, null);

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(model, times(cacheEnabled ? 1 : 2)).call(captor.capture());
        String systemPrompt = new ClassPathResource("prompts/musicbrainz-album-normalization.txt").getContentAsString(UTF_8);
        for (int index = 0; index < captor.getAllValues().size(); index++) {
            Prompt prompt = captor.getAllValues().get(index);
            OpenAiChatOptions options = (OpenAiChatOptions) prompt.getOptions();
            assertThat(options.getToolCallbacks()).isNullOrEmpty();
            assertThat(options.getModel()).isEqualTo("test");
            assertThat(options.getTemperature()).isEqualTo(0.0);
            assertThat(options.getToolChoice()).isEqualTo("none");
            assertThat(options.getResponseFormat().getType()).isEqualTo(ResponseFormat.Type.JSON_SCHEMA);
            assertThat(options.getResponseFormat().getStrict()).isTrue();
            assertThat(prompt.getInstructions()).hasSize(3);
            assertThat(prompt.getInstructions().getFirst().getText()).isEqualTo(systemPrompt);
            assertThat(JsonConverter.fromJson(prompt.getInstructions().get(1).getText(), Map.class))
                    .isEqualTo(Map.of("albumTitle", album.getName()));
            assertThat(prompt.getInstructions().getLast().getText()).contains("Collect all data needed");
            Map<?, ?> schema = JsonConverter.fromJson(options.getResponseFormat().getJsonSchema(), Map.class);
            assertThat(schema.get("required")).isEqualTo(List.of("searchTitle", "searchFragments", "alternativeTitles"));
            assertThat(schema.get("additionalProperties")).isEqualTo(false);
            Map<?, ?> properties = (Map<?, ?>) schema.get("properties");
            assertThat(((Map<?, ?>) properties.get("searchTitle")).get("type")).isEqualTo("string");
            assertThat(((Map<?, ?>) properties.get("searchFragments")).get("type")).isEqualTo("array");
            Map<?, ?> alternativeTitles = (Map<?, ?>) properties.get("alternativeTitles");
            assertThat(alternativeTitles.get("type")).isEqualTo("array");
            assertThat(((Map<?, ?>) alternativeTitles.get("items")).get("type")).isEqualTo("string");
        }
        assertThat(taskRepository.findAll()).hasSize(2).allSatisfy(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(JsonConverter.fromJson(task.getResult(), MusicBrainzAlbumData.class).releaseGroupId()).isEqualTo("group");
            Object[] requests = JsonConverter.fromJson(task.getRawRequest(), Object[].class);
            assertThat(requests).hasSize(4);
            assertThat(requests[0]).isEqualTo(SEARCH_URL);
            assertThat(requests[1]).isInstanceOf(Map.class);
            Map<?, ?> snapshot = (Map<?, ?>) requests[1];
            assertThat(snapshot.get("systemPrompt")).isEqualTo(systemPrompt);
            assertThat(snapshot.get("model")).isEqualTo("test");
            assertThat(snapshot.get("temperature")).isEqualTo(0.0);
            assertThat(snapshot.get("toolChoice")).isEqualTo("none");
            assertThat(snapshot.get("responseSchema")).isInstanceOf(Map.class);
            assertThat(requests[2]).isEqualTo(NORMALIZED_SEARCH_URL);
            assertThat(requests[3]).isEqualTo(ARTIST_URL);
            assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class))
                    .containsExactly(EMPTY_SEARCH_RESPONSE, normalization, rawSearchResponse, rawArtistResponse);
        });
        verify(session, times(2)).searchReleaseGroups("artistname:\"Slayer\" AND release:\"Reign\"");
        verify(session, times(4)).searchReleaseGroups(anyString());
        assertThat(cacheRepository.findAll()).hasSize(cacheEnabled ? 1 : 0).allSatisfy(entry -> {
            assertThat(entry.getRegion()).isEqualTo(MUSICBRAINZ);
            assertThat(entry.getKey()).startsWith("MUSICBRAINZ_ALBUM_TITLE_NORMALIZATION:");
        });
    }

    @ParameterizedTest
    @MethodSource("normalizationQueries")
    void shouldBuildNormalizedQueryAndStopAfterSecondEmptySearch(String original, String title, List<String> fragments, String expected) {
        enableLlm();
        albumRepository.save(album.setName(original));
        searchAfterEmpty(List.of());
        String normalization = JsonConverter.toJson(Map.of("searchTitle", title, "searchFragments", fragments, "alternativeTitles", List.of()));
        when(model.call(any(Prompt.class))).thenReturn(response(normalization));

        albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(onlyTask().getResult()).isEqualTo("null");
        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(session, times(expected == null ? 1 : 2)).searchReleaseGroups(queries.capture());
        if (expected != null) {
            assertThat(queries.getAllValues().getLast()).isEqualTo("artistname:\"Slayer\" AND " + expected);
        }
        verify(model, times(1)).call(any(Prompt.class));
        verify(session, never()).getArtist(anyString());
    }

    private static Stream<Arguments> normalizationQueries() {
        return Stream.of(
                Arguments.of("Memento Mori (CD1 of 2) [WAV+CUE]", "Memento Mori", List.of(), "release:\"Memento Mori\""),
                Arguments.of("No. 4: Black & White [FLAC]", "No. 4: Black & White", List.of("Black & White"),
                        "(release:\"No. 4\\: Black \\& White\" OR release:\"Black \\& White\" "
                                + "OR release:\"No 4 Black White\" OR release:\"Black White\")"),
                Arguments.of("Collecting Data MM.V.I: Spine Coated Catalyst", "Collecting Data MM.V.I: Spine Coated Catalyst",
                        List.of("Collecting Data", "Spine Coated Catalyst"),
                        "((release:\"Collecting Data\" AND release:\"Spine Coated Catalyst\") "
                                + "OR release:\"Collecting Data MM V I Spine Coated Catalyst\" "
                                + "OR release:\"Collecting Data MMVI Spine Coated Catalyst\")"),
                Arguments.of("L'Éternité…", "L'Éternité…", List.of("L'Éternité"),
                        "(release:\"L'Éternité\" OR release:\"L Éternité\" OR release:\"LÉternité\")"),
                Arguments.of("Who? What! Why... [FLAC]", "Who? What! Why...", List.of("Who", "What", "Why"),
                        "(release:\"Who\\? What\\! Why...\" OR (release:\"Who\" AND release:\"What\" AND release:\"Why\") "
                                + "OR release:\"Who What Why\")"),
                Arguments.of("Dead//Alive: Vol. 1 [FLAC]", "Dead//Alive: Vol. 1", List.of("Dead", "Alive"),
                        "(release:\"Dead\\/\\/Alive\\: Vol. 1\" OR (release:\"Dead\" AND release:\"Alive\") "
                                + "OR release:\"Dead Alive Vol 1\" OR release:\"DeadAlive Vol 1\")"),
                Arguments.of("EP (The Album)", "EP (The Album)", List.of(), "release:\"EP The Album\""),
                Arguments.of("Title", "Title", List.of("Title", "Title"), null),
                Arguments.of("Title [FLAC]", "Title", List.of("Title", "Title"), "release:\"Title\""),
                Arguments.of("Title", "Title: Subtitle", List.of("Title"),
                        "(release:\"Title\\: Subtitle\" OR release:\"Title Subtitle\")"),
                Arguments.of("Title: Subtitle", "Title: Subtitle", List.of("Title", "Title", "Subtitle"),
                        "((release:\"Title\" AND release:\"Subtitle\") OR release:\"Title Subtitle\")"),
                Arguments.of("No. 4: Black & White", "No. 4: Black & White", List.of("No. 4"),
                        "(release:\"No. 4\" OR release:\"No 4 Black White\" OR release:\"No 4\")"),
                Arguments.of("Submit To Self-Destruction", "Submit To Self-Destruction", List.of(),
                        "(release:\"Submit To Self Destruction\" OR release:\"Submit To SelfDestruction\")"),
                Arguments.of("Submit To Self-Destruction [FLAC]", "Submit To Self-Destruction", List.of(),
                        "(release:\"Submit To Self\\-Destruction\" OR release:\"Submit To Self Destruction\" "
                                + "OR release:\"Submit To SelfDestruction\")"),
                Arguments.of("Alpha-Beta / Gamma.Delta [FLAC]", "Alpha-Beta / Gamma.Delta", List.of("Alpha-Beta", "Gamma.Delta"),
                        "(release:\"Alpha\\-Beta \\/ Gamma.Delta\" OR (release:\"Alpha\\-Beta\" AND release:\"Gamma.Delta\") "
                                + "OR release:\"Alpha Beta Gamma Delta\" OR (release:\"Alpha Beta\" AND release:\"Gamma Delta\") "
                                + "OR release:\"AlphaBeta GammaDelta\" OR (release:\"AlphaBeta\" AND release:\"GammaDelta\"))"),
                Arguments.of("A-B A/B", "A-B A/B", List.of("A-B", "A/B"),
                        "((release:\"A\\-B\" AND release:\"A\\/B\") OR release:\"A B A B\" OR release:\"A B\" "
                                + "OR release:\"AB AB\" OR release:\"AB\")"),
                Arguments.of("Été-東京_Группа ١２", "Été-東京_Группа ١２", List.of(),
                        "(release:\"Été 東京 Группа ١２\" OR release:\"Été東京Группа ١２\")"),
                Arguments.of("  Submit  To  Self--Destruction!  ", "  Submit  To  Self--Destruction!  ", List.of(),
                        "(release:\"Submit To Self Destruction\" OR release:\"Submit To SelfDestruction\")"),
                Arguments.of("---", "---", List.of(), null),
                Arguments.of("---", "---", List.of("-"), "release:\"\\-\""),
                Arguments.of("Alpha & Beta", "Alpha & Beta", List.of("Alpha", "&", "Beta"),
                        "((release:\"Alpha\" AND release:\"\\&\" AND release:\"Beta\") OR release:\"Alpha Beta\")"));
    }

    @ParameterizedTest
    @MethodSource("alternativeTitleQueries")
    void shouldSearchAlternativeTitlesIndependentlyOfAndFragments(
            String original, String title, List<String> fragments, List<String> alternativeTitles, String expected
    ) {
        enableLlm();
        albumRepository.save(album.setName(original));
        searchAfterEmpty(List.of());
        String normalization = JsonConverter.toJson(Map.of(
                "searchTitle", title, "searchFragments", fragments, "alternativeTitles", alternativeTitles));
        when(model.call(any(Prompt.class))).thenReturn(response(normalization));

        albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(onlyTask().getResult()).isEqualTo("null");
        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(session, times(expected == null ? 1 : 2)).searchReleaseGroups(queries.capture());
        if (expected != null) {
            assertThat(queries.getAllValues().getLast()).isEqualTo("artistname:\"Slayer\" AND " + expected);
        }
        verify(model).call(any(Prompt.class));
        verify(session, never()).getArtist(anyString());
    }

    private static Stream<Arguments> alternativeTitleQueries() {
        return Stream.of(
                Arguments.of("Th1rt3en (Thirteen)", "Th1rt3en (Thirteen)", List.of(), List.of("Thirteen", "Th1rt3en"),
                        "(release:\"Thirteen\" OR release:\"Th1rt3en\" OR release:\"Th1rt3en Thirteen\")"),
                Arguments.of("Th1rt3en (Thirteen) [FLAC]", "Th1rt3en", List.of(), List.of("Thirteen"),
                        "(release:\"Th1rt3en\" OR release:\"Thirteen\")"),
                Arguments.of("Alpha//Beta (Gamma)", "Alpha//Beta (Gamma)", List.of("Alpha", "Beta"), List.of("Gamma"),
                        "(release:\"Gamma\" OR (release:\"Alpha\" AND release:\"Beta\") "
                                + "OR release:\"Alpha Beta Gamma\" OR release:\"AlphaBeta Gamma\")"),
                Arguments.of("Title (Alias)", "Title", List.of("Title"), List.of("Title", "Alias", "Alias"),
                        "(release:\"Title\" OR release:\"Alias\")"),
                Arguments.of("Title", "Title", List.of(), List.of("Title"), null),
                Arguments.of("A&B (C/D)", "A&B (C/D)", List.of(), List.of("A&B", "C/D"),
                        "(release:\"A\\&B\" OR release:\"C\\/D\" OR release:\"A B C D\" "
                                + "OR release:\"A B\" OR release:\"C D\" OR release:\"AB CD\" OR release:\"AB\" OR release:\"CD\")"),
                Arguments.of("Été (東京)", "Été", List.of(), List.of("東京"),
                        "(release:\"Été\" OR release:\"東京\")"),
                Arguments.of("Title (*)", "Title (*)", List.of(), List.of("*"),
                        "(release:\"\\*\" OR release:\"Title\")"));
    }

    @ParameterizedTest
    @MethodSource("invalidNormalizations")
    void shouldVerifyInvalidNormalizationAndCacheOnlyAcceptedResponse(String rejected, String diagnostic) {
        enableLlm();
        search(List.of());
        String accepted = "{\"searchTitle\":\"Reign in Blood\",\"searchFragments\":[],\"alternativeTitles\":[]}";
        AtomicInteger calls = new AtomicInteger();
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            assertThat(cacheRepository.count()).isZero();
            return response(calls.incrementAndGet() == 1 ? rejected : accepted);
        });

        albumService.discover(job(DiscoveryType.ALBUM), album, true, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(JsonConverter.fromJson(onlyTask().getRawResult(), String[].class))
                .containsExactly(rawSearchResponse, rejected, accepted);
        assertThat(cacheRepository.findAll()).singleElement().satisfies(entry ->
                assertThat(JsonConverter.mapFromJson(entry.getValue(), String.class, Object.class).get("response")).isEqualTo(accepted));
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(model, times(2)).call(captor.capture());
        assertThat(captor.getAllValues().get(1).getInstructions()).anySatisfy(message ->
                assertThat(message.getText()).contains("Your previous response was invalid", diagnostic, "searchTitle", "searchFragments", "alternativeTitles")
                        .doesNotContain("Always include id"));
        assertThat(logRepository.findAll()).anySatisfy(log -> {
            assertThat(log.getText()).contains(artist.getName(), album.getName(), "album title normalization");
            assertThat(log.getArguments().getLast()).contains(diagnostic, "\tat ");
        });

        albumService.discover(job(DiscoveryType.ALBUM), album, true, null);

        verify(model, times(2)).call(any(Prompt.class));
        verify(session, times(2)).searchReleaseGroups(anyString());
    }

    private static Stream<Arguments> invalidNormalizations() {
        return Stream.of(
                Arguments.of("not JSON", "Unrecognized token"),
                Arguments.of("null", "must be a JSON object"),
                Arguments.of("[]", "must be a JSON object"),
                Arguments.of("{}", "exactly searchTitle, searchFragments and alternativeTitles"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":[]}",
                        "exactly searchTitle, searchFragments and alternativeTitles"),
                Arguments.of("{\"searchTitle\":\"Title\",\"alternativeTitles\":[]}", "exactly searchTitle, searchFragments and alternativeTitles"),
                Arguments.of("{\"searchFragments\":[],\"alternativeTitles\":[]}", "exactly searchTitle, searchFragments and alternativeTitles"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":[],\"extra\":true,\"alternativeTitles\":[]}", "exactly searchTitle, searchFragments and alternativeTitles"),
                Arguments.of("{\"searchTitle\":null,\"searchFragments\":[],\"alternativeTitles\":[]}", "searchTitle must be a non-blank string"),
                Arguments.of("{\"searchTitle\":123,\"searchFragments\":[],\"alternativeTitles\":[]}", "searchTitle must be a non-blank string"),
                Arguments.of("{\"searchTitle\":false,\"searchFragments\":[],\"alternativeTitles\":[]}", "searchTitle must be a non-blank string"),
                Arguments.of("{\"searchTitle\":\"\",\"searchFragments\":[],\"alternativeTitles\":[]}", "searchTitle must be a non-blank string"),
                Arguments.of("{\"searchTitle\":\" \\t\",\"searchFragments\":[],\"alternativeTitles\":[]}", "searchTitle must be a non-blank string"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":null,\"alternativeTitles\":[]}", "searchFragments must be an array"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":\"Title\",\"alternativeTitles\":[]}", "searchFragments must be an array"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":[\"T\",\"i\",\"t\",\"l\"],\"alternativeTitles\":[]}", "at most three"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":[null],\"alternativeTitles\":[]}", "item must be a non-blank string"),
                Arguments.of("{\"searchTitle\":\"123\",\"searchFragments\":[123],\"alternativeTitles\":[]}", "item must be a non-blank string"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":[\"\"],\"alternativeTitles\":[]}", "item must be a non-blank string"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":[\" \"],\"alternativeTitles\":[]}", "item must be a non-blank string"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":[\"title\"],\"alternativeTitles\":[]}", "literal contiguous substring"),
                Arguments.of("{\"searchTitle\":\"Title: Subtitle\",\"searchFragments\":[\"Title Subtitle\"],\"alternativeTitles\":[]}", "literal contiguous substring"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":[],\"alternativeTitles\":null}",
                        "alternativeTitles must be an array"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":[],\"alternativeTitles\":\"Title\"}",
                        "alternativeTitles must be an array"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":[],\"alternativeTitles\":[\"a\",\"b\",\"c\",\"d\"]}",
                        "alternativeTitles must contain at most three"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":[],\"alternativeTitles\":[null]}",
                        "Each alternativeTitles item must be a non-blank string"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":[],\"alternativeTitles\":[123]}",
                        "Each alternativeTitles item must be a non-blank string"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":[],\"alternativeTitles\":[\"\"]}",
                        "Each alternativeTitles item must be a non-blank string"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":[],\"alternativeTitles\":[\" \"]}",
                        "Each alternativeTitles item must be a non-blank string"),
                Arguments.of("{\"searchTitle\":\"Title\",\"searchFragments\":[],\"alternativeTitles\":[\"Title\"]}",
                        "Each alternativeTitles item must be a literal contiguous substring of albumTitle"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"model", "prompt", "title"})
    void shouldSeparateNormalizationCacheByModelPromptAndOriginalTitle(String change) throws IOException {
        enableLlm();
        search(List.of());
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            String title = (String) JsonConverter.fromJson(prompt.getInstructions().get(1).getText(), Map.class).get("albumTitle");
            return response(JsonConverter.toJson(Map.of("searchTitle", title, "searchFragments", List.of(), "alternativeTitles", List.of())));
        });
        String originalPrompt = new ClassPathResource("prompts/musicbrainz-album-normalization.txt").getContentAsString(UTF_8);
        try {
            albumService.discover(job(DiscoveryType.ALBUM), album, true, null);
            albumService.discover(job(DiscoveryType.ALBUM), album, true, null);
            verify(model, times(1)).call(any(Prompt.class));
            String originalKey = cacheRepository.findAll().getFirst().getKey();
            switch (change) {
                case "model" -> configService.update(new ConfigSetUpdateCommand()
                        .setDiscoveryEnabled(true).setLlmUrl("http://localhost:11434/v1").setLlmModel("different-model"));
                case "prompt" -> ReflectionTestUtils.setField(musicBrainzAlbumDataService, "normalizationPrompt", originalPrompt + "\n");
                case "title" -> albumRepository.save(album.setName("Different title"));
                default -> throw new IllegalArgumentException(change);
            }

            albumService.discover(job(DiscoveryType.ALBUM), album, true, null);

            verify(model, times(2)).call(any(Prompt.class));
            assertThat(cacheRepository.findAll()).hasSize(change.equals("model") ? 1 : 2)
                    .extracting(DiscoveryCache::getKey).doesNotHaveDuplicates();
            assertThat(cacheRepository.findAll()).anySatisfy(entry -> assertThat(entry.getKey()).isNotEqualTo(originalKey));
            assertThat(taskRepository.findAll()).hasSize(3)
                    .allSatisfy(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE));
        } finally {
            ReflectionTestUtils.setField(musicBrainzAlbumDataService, "normalizationPrompt", originalPrompt);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldFailNormalizationInsteadOfReturningNotFound(boolean invalidResponse) {
        enableLlm();
        search(List.of());
        if (invalidResponse) {
            when(model.call(any(Prompt.class))).thenReturn(response("{\"searchTitle\":123,\"searchFragments\":[],\"alternativeTitles\":[]}"));
        } else {
            when(model.call(any(Prompt.class))).thenThrow(new IllegalStateException("Normalization unavailable"));
        }

        albumService.discover(job(DiscoveryType.ALBUM), album, true, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
        assertThat(onlyTask().getResult()).contains(invalidResponse ? "searchTitle must be a non-blank string" : "Normalization unavailable");
        assertThat(JsonConverter.fromJson(onlyTask().getRawResult(), String[].class)).hasSize(invalidResponse ? 5 : 2);
        assertThat(cacheRepository.count()).isZero();
        verify(session, times(1)).searchReleaseGroups(anyString());
        verify(model, times(invalidResponse ? 4 : 1)).call(any(Prompt.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"collection", "search"})
    void shouldCancelNormalizationAndAdditionalSearch(String phase) {
        enableLlm();
        searchAfterEmpty(groups);
        String normalization = "{\"searchTitle\":\"Reign\",\"searchFragments\":[],\"alternativeTitles\":[]}";
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            if (phase.equals("collection")) {
                jobSynchronizer.cancelDiscovery();
            }
            return response(normalization);
        });
        if (phase.equals("search")) {
            doAnswer(invocation -> {
                exchangeObserver.onRequest(NORMALIZED_SEARCH_URL);
                jobSynchronizer.cancelDiscovery();
                throw new MusicBrainzClient.CancelledException();
            }).when(session).searchReleaseGroups("artistname:\"Slayer\" AND release:\"Reign\"");
        }

        assertThatThrownBy(() -> albumService.discover(job(DiscoveryType.ALBUM), album, true, null))
                .isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.INTERRUPTED);
        assertThat(cacheRepository.count()).isEqualTo(phase.equals("search") ? 1 : 0);
        verify(model).call(any(Prompt.class));
        verify(session, times(phase.equals("search") ? 2 : 1)).searchReleaseGroups(anyString());
        verify(session).cancel();
        verify(session).close();
        verify(session, never()).getArtist(anyString());
    }

    @Test
    void shouldSelectNewCandidatesWithOriginalAlbumAndTracksAfterNormalization() {
        enableLlm();
        albumRepository.save(album.setName("Reign in Blood (CD1 of 2)"));
        Genre genre = genreRepository.save(new Genre().setName("Genre"));
        songRepository.save(song().setName("Original track").setAlbum(album).setGenre(genre));
        searchAfterEmpty(List.of(group("normalized-group", "Other", "Reign in Blood",
                List.of(credit("artist", "A", null), credit("other", "B", null)))));
        String normalization = "{\"searchTitle\":\"Reign in Blood\",\"searchFragments\":[],\"alternativeTitles\":[]}";
        when(model.call(any(Prompt.class))).thenReturn(response(normalization),
                response("{\"id\":\"outside-new-candidates\"}"),
                response("{\"id\":\"normalized-group\"}"),
                response("{\"id\":\"artist\"}"));

        albumService.discover(job(DiscoveryType.ALBUM), album, true, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(JsonConverter.fromJson(onlyTask().getResult(), MusicBrainzAlbumData.class).releaseGroupId()).isEqualTo("normalized-group");
        assertThat(albumRepository.findById(album.getId()).orElseThrow().getName()).isEqualTo("Reign in Blood (CD1 of 2)");
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(model, times(4)).call(captor.capture());
        assertThat(captor.getAllValues().get(1).getInstructions().get(1).getText())
                .contains("Reign in Blood (CD1 of 2)", "1986", "Slayer", "Original track", "normalized-group")
                .doesNotContain(EMPTY_SEARCH_RESPONSE);
        assertThat(captor.getAllValues().get(3).getInstructions().get(1).getText())
                .contains("Reign in Blood (CD1 of 2)", "Slayer", "normalized-group")
                .doesNotContain(EMPTY_SEARCH_RESPONSE);
        assertThat(JsonConverter.fromJson(onlyTask().getRawResult(), String[].class)).containsExactly(
                EMPTY_SEARCH_RESPONSE, normalization, rawSearchResponse, "{\"id\":\"outside-new-candidates\"}",
                "{\"id\":\"normalized-group\"}", "{\"id\":\"artist\"}", rawArtistResponse);
        verify(session).getArtist("artist");
    }

    @Test
    void shouldEscapeNormalizedQueryAndUrlEncodeUnicodeOnlyOnce() throws IOException {
        enableLlm();
        artistRepository.save(artist.setName("A&B / \"Группа\"\\"));
        albumRepository.save(album.setName("Исходное \"название\" \\ [FLAC]"));
        String title = "L'Éternité… \"B\" \\ C/D: & + - ! () {} [] ^ ~ * ? |";
        String normalization = JsonConverter.toJson(Map.of("searchTitle", title, "searchFragments", List.of("L'Éternité"), "alternativeTitles", List.of()));
        when(model.call(any(Prompt.class))).thenReturn(response(normalization));
        ConcurrentLinkedQueue<String> queries = new ConcurrentLinkedQueue<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ws/2/", exchange -> {
            try (exchange) {
                queries.add(URLDecoder.decode(exchange.getRequestURI().getRawQuery(), UTF_8));
                byte[] body = EMPTY_SEARCH_RESPONSE.getBytes(UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
        });
        MusicBrainzClient client = new MusicBrainzClient(cacheService,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/ws/2/", Duration.ZERO, Duration.ZERO, 0, Duration.ofSeconds(10), 25);
        when(musicBrainzClient.openSession(anyString(), any(), anyBoolean())).thenAnswer(invocation ->
                client.openSession(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
        server.start();
        try {
            albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

            assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(queries).hasSize(2);
            assertThat(new ArrayList<>(queries).getLast()).contains(
                    "query=artistname:\"A\\&B \\/ \\\"Группа\\\"\\\\\" AND "
                            + "(release:\"L'Éternité… \\\"B\\\" \\\\ C\\/D\\: \\& \\+ \\- \\! \\(\\) \\{\\} \\[\\] \\^ \\~ \\* \\? \\|\" "
                            + "OR release:\"L'Éternité\" OR release:\"L Éternité B C D\" OR release:\"L Éternité\" "
                            + "OR release:\"LÉternité B CD\" OR release:\"LÉternité\")");
            ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
            verify(model, times(1)).call(captor.capture());
            assertThat(JsonConverter.fromJson(captor.getAllValues().getFirst().getInstructions().get(1).getText(), Map.class))
                    .isEqualTo(Map.of("albumTitle", album.getName()));
        } finally {
            client.onApplicationShutdown();
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"id\":\"artist\",\"country\":\"DE\"}",
            "{\"id\":\"artist\",\"country\":\"DE\",\"area\":null}",
            "{\"id\":\"artist\",\"country\":\"DE\",\"area\":{\"name\":\"Würzburg\"}}",
            "{\"id\":\"artist\",\"country\":\"DE\",\"area\":{\"iso-3166-1-codes\":[]}}"
    })
    void shouldFallBackToArtistCountryWhenAreaHasNoCountryCodes(String response) {
        rawArtistResponse = response;
        artistService.discover(job(DiscoveryType.ARTIST), artist, false, null);

        assertThat(JsonConverter.fromJson(albumTasks(album).getFirst().getResult(), MusicBrainzAlbumData.class).country())
                .containsExactly("DE");
        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get()
                .extracting(ArtistDiscoveryIndex::getCountryCode).isEqualTo("DE");
    }

    @Test
    void shouldPreferAreaCountryCodesOverArtistCountry() {
        rawArtistResponse = "{\"id\":\"artist\",\"country\":\"DE\",\"area\":{\"iso-3166-1-codes\":[\"US\",\"GB\"]}}";
        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(JsonConverter.fromJson(onlyTask().getResult(), MusicBrainzAlbumData.class).country()).containsExactly("US", "GB");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"id\":\"artist\"}",
            "{\"id\":\"artist\",\"country\":null}",
            "{\"id\":\"artist\",\"country\":\"\"}",
            "{\"id\":\"artist\",\"country\":\" \"}",
            "{\"id\":\"artist\",\"area\":{\"name\":\"Würzburg\"}}"
    })
    void shouldUseEmptyCountryArrayWhenCountryIsUnavailable(String response) {
        rawArtistResponse = response;
        albumService.discover(job(DiscoveryType.ALBUM), album, null);

        assertThat(JsonConverter.fromJson(onlyTask().getResult(), MusicBrainzAlbumData.class).country()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "The Bridge City Sinners, Bridge City Sinners",
            "the Bridge City Sinners, Bridge City Sinners",
            "THE Bridge City Sinners, Bridge City Sinners",
            "The Bridge City The Sinners, Bridge City The Sinners",
            "Bridge City Sinners, Bridge City Sinners",
            "Rage Against the Machine, Rage Against the Machine",
            "Theocracy, Theocracy",
            "The The, The",
            "the the, the",
            "The, The",
            "'The ', 'The '"
    })
    void shouldNormalizeArtistNameOnlyForInitialAndFallbackSearch(String artistName, String searchName) {
        enableLlm();
        artistRepository.save(artist.setName(artistName));
        searchAfterEmpty(groups);
        String normalization = "{\"searchTitle\":\"Reign in Blood\",\"searchFragments\":[\"Reign\"],\"alternativeTitles\":[]}";
        when(model.call(any(Prompt.class))).thenReturn(response(normalization));

        albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(JsonConverter.fromJson(onlyTask().getResult(), MusicBrainzAlbumData.class).releaseGroupId()).isEqualTo("group");
        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(session, times(2)).searchReleaseGroups(queries.capture());
        assertThat(queries.getAllValues()).containsExactly(
                "release:\"Reign in Blood\" AND artistname:\"" + searchName + "\"",
                "artistname:\"" + searchName + "\" AND release:\"Reign\"");
        assertThat(artistRepository.findById(artist.getId()).orElseThrow().getName()).isEqualTo(artistName);
    }

    @Test
    void shouldSkipMissingNamesAndEscapeLuceneSpecialCharacters() {
        notFoundResponses();
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
        enableLlm();
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
        verify(model, never()).call(any(Prompt.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldDiscoverEveryAlbumBeforeSpotifyTopTracksAndKeepArtistProgress(boolean discoveryEnabled) {
        notFoundResponses();
        if (discoveryEnabled) {
            enableLlm();
        }
        Album second = albumRepository.save(new Album().setArtist(artist).setName("Second album"));
        search(List.of());
        List<DiscoveryProgress> progress = new ArrayList<>();
        DiscoveryJob job = job(DiscoveryType.ARTIST);

        artistService.discover(job, artist, false, progress::add);

        int total = 4;
        assertThat(progress).hasSize(total + 1);
        for (int index = 0; index <= total; index++) {
            assertThat(progress.get(index).getStep()).isEqualTo(DiscoveryProgress.Step.ARTIST_DISCOVERY);
            assertThat(progress.get(index).getValue()).isEqualTo(DiscoveryProgress.Value.of(index, total));
        }
        List<DiscoveryTask> tasks = taskRepository.findAll().stream().sorted(Comparator.comparing(DiscoveryTask::getCreationDate)).toList();
        assertThat(tasks).hasSize(total).allSatisfy(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE));
        assertThat(tasks).extracting(DiscoveryTask::getType).containsExactly(DiscoveryTaskType.SPOTIFY_ARTIST_DATA,
                DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTaskType.SPOTIFY_TOP_TRACKS);
        getTransactionTemplate().executeWithoutResult(status -> {
            assertThat(artistTasks()).hasSize(2);
            assertThat(taskRepository.findAll().stream().filter(value -> value.getAlbum() != null))
                    .extracting(value -> value.getAlbum().getId()).containsExactlyInAnyOrder(album.getId(), second.getId());
        });
    }

    @ParameterizedTest
    @CsvSource({"ARTIST,Album,,true", "FULL,EP,,true", "ARTIST,Album,Live,false", "ARTIST,EP,Compilation,true"})
    void shouldUseCompletedMusicBrainzDataForTopTracksFallback(DiscoveryType type, String primaryType,
                                                               String secondaryType, boolean eligible) {
        enableLlm();
        MusicBrainzDto.ReleaseGroup original = groups.getFirst();
        search(List.of(new MusicBrainzDto.ReleaseGroup(original.id(), original.title(), primaryType,
                secondaryType != null ? List.of(secondaryType) : List.of(), original.firstReleaseDate(),
                original.artistCredit(), original.releases())));
        Genre genre = genreRepository.save(new Genre().setName("Metal"));
        Song track = songRepository.save(song().setPath("fallback-track").setName("Raining Blood").setAlbum(album).setGenre(genre));
        SpotifyArtistData spotifyData = new SpotifyArtistData(SpotifyArtistData.Status.FOUND,
                new SpotifyArtistData.SpotifyArtist("spotify-artist", "Slayer", "https://open.spotify.com/artist/spotify-artist"),
                new SpotifyArtistData.MatchedAlbum("Reign in Blood", "Reign in Blood", "spotify-lp", "https://open.spotify.com/album/spotify-lp"),
                List.of(new SpotifyArtistData.TopTrack("spotify-track", "Raining Blood", "https://open.spotify.com/track/spotify-track",
                        "Compilation", "spotify-compilation", "https://open.spotify.com/album/spotify-compilation")),
                null, null, null, null, null);
        List<String> responses = new ArrayList<>(List.of(JsonConverter.toJson(spotifyData),
                "{\"albumMatches\":[{\"spotifyAlbumId\":\"spotify-compilation\",\"databaseAlbumId\":null}]}",
                "{\"trackMatches\":[{\"databaseTrackId\":null}]}",
                "{\"trackMatches\":[{\"databaseTrackId\":\"" + track.getId() + "\"}]}"));
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            Prompt prompt = invocation.getArgument(0);
            if (prompt.getInstructions().getFirst().getText().startsWith("Match the supplied")) {
                assertThat(albumTasks(album)).singleElement().satisfies(task -> {
                    assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
                    assertThat(JsonConverter.fromJson(task.getResult(), MusicBrainzAlbumData.class).primaryType()).isEqualTo(primaryType);
                });
            }
            String result = ((OpenAiChatOptions) prompt.getOptions()).getResponseFormat().getType() == ResponseFormat.Type.TEXT
                    ? responses.getFirst() : responses.removeFirst();
            return response(result);
        });
        DiscoveryJob discoveryJob = job(type);

        if (type == DiscoveryType.FULL) {
            fullDiscoveryService.discover(discoveryJob, false, null);
        } else {
            artistService.discover(discoveryJob, artist, false, null);
        }

        assertThat(taskRepository.findAll().stream().filter(task -> task.getType() == DiscoveryTaskType.SPOTIFY_TOP_TRACKS))
                .singleElement().satisfies(task -> {
                    assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
                    if (eligible) {
                        assertThat(JsonConverter.listFromJson(task.getResult(), String.class)).containsExactly(track.getId());
                    } else {
                        assertThat(task.getResult()).isEqualTo("null");
                    }
                    assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).hasSize(eligible ? 3 : 2);
                });
        verify(model, times(eligible ? 8 : 6)).call(any(Prompt.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReportFullDiscoveryProgressInTaskCounts(boolean discoveryEnabled) {
        notFoundResponses();
        if (discoveryEnabled) {
            enableLlm();
        }
        albumRepository.save(new Album().setArtist(artist).setName("Second album"));
        search(List.of());
        DiscoveryJob job = job(DiscoveryType.FULL);
        int total = 4;
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(job, false, progress -> {
            progressCalls.add(progress);
            assertThat(progress.getStep()).isEqualTo(DiscoveryProgress.Step.FULL_DISCOVERY);
            assertThat(progress.getValue()).isEqualTo(DiscoveryProgress.Value.of(
                    taskRepository.countByJobIdAndStatus(job.getId(), DiscoveryTask.Status.COMPLETE), total));
        });

        assertThat(progressCalls).hasSize(total + 3);
        assertThat(progressCalls.getFirst().getValue()).isEqualTo(DiscoveryProgress.Value.of(0, total));
        assertThat(progressCalls.getLast().getValue()).isEqualTo(DiscoveryProgress.Value.of(total, total));
        assertThat(taskRepository.findAll()).hasSize(total)
                .allSatisfy(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE));
    }

    @ParameterizedTest
    @CsvSource({"0,1", "2,2", "3,4"})
    void shouldCancelFullDiscoveryWhenAllPartsAreAlreadyComplete(long completedTasksBeforeCancellation, int expectedProgressCalls) {
        enableLlm();
        DiscoveryJob previousJob = jobRepository.save(job(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.COMPLETE));
        saveDiscoveryHistory(previousJob, 2, 1);
        DiscoveryJob nextJob = job(DiscoveryType.FULL);
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        assertThatThrownBy(() -> fullDiscoveryService.discover(nextJob, false, progress -> {
            progressCalls.add(progress);
            if (progress.getValue().getItemsComplete() == completedTasksBeforeCancellation) {
                jobSynchronizer.cancelDiscovery();
            }
        })).isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(progressCalls).hasSize(expectedProgressCalls);
        assertThat(progressCalls.getLast().getValue()).isEqualTo(DiscoveryProgress.Value.of(completedTasksBeforeCancellation, 3));
        assertThat(taskRepository.countByJobId(nextJob.getId())).isZero();
        assertThat(artistTasks()).hasSize(2);
        assertThat(albumTasks(album)).hasSize(1);
        assertThat(jobSynchronizer.hasRunningTasks()).isFalse();
        verifyNoInteractions(musicBrainzClient);
        verify(model, never()).call(any(Prompt.class));
    }

    @ParameterizedTest
    @CsvSource({
            "INTERRUPTED,-1,1,2", "INTERRUPTED,0,0,3", "INTERRUPTED,1,0,2",
            "INTERRUPTED,2,-1,1", "INTERRUPTED,2,0,1", "INTERRUPTED,2,1,0",
            "MODERATE,1,1,1", "MODERATE,2,0,1", "COMPLETE,2,1,0"
    })
    void shouldResumeFullDiscoveryByCompletedTaskCounts(DiscoveryJob.Status previousStatus, int artistTasks,
                                                       int albumTasks, int expectedNewTasks) {
        enableLlm();
        notFoundResponses();
        DiscoveryJob previousJob = jobRepository.save(job(DiscoveryType.FULL).setStatus(previousStatus));
        saveDiscoveryHistory(previousJob, artistTasks, albumTasks);
        search(List.of());
        DiscoveryJob nextJob = job(DiscoveryType.FULL);
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(nextJob, false, progressCalls::add);

        assertThat(taskRepository.countByJobId(nextJob.getId())).isEqualTo(expectedNewTasks);
        assertThat(artistTasks()).hasSize(2);
        assertThat(albumTasks(album)).hasSize(Math.max(albumTasks, 0) + (albumTasks < 1 ? 1 : 0));
        assertThat(progressCalls.getFirst().getValue()).isEqualTo(DiscoveryProgress.Value.of(0, 3));
        assertThat(progressCalls.getLast().getValue()).isEqualTo(DiscoveryProgress.Value.of(3, 3));
    }

    @ParameterizedTest
    @EnumSource(value = DiscoveryTask.Status.class, names = {"STARTED", "FAILED", "INTERRUPTED"})
    void shouldCountOnlyCompleteTasksWhenResumingFullDiscovery(DiscoveryTask.Status incompleteStatus) {
        enableLlm();
        notFoundResponses();
        DiscoveryJob previousJob = jobRepository.save(job(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.INTERRUPTED));
        DiscoveryTask completedTask = task(previousJob, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE);
        DiscoveryTask incompleteArtistTask = task(previousJob, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, incompleteStatus);
        DiscoveryTask incompleteAlbumTask = task(previousJob, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, incompleteStatus);
        search(List.of());
        doAnswer(invocation -> {
            exchangeObserver.onRequest(SEARCH_URL);
            exchangeObserver.onResponse(SEARCH_URL, rawSearchResponse);
            return new MusicBrainzClient.ReleaseGroupSearchResult(groups, rawSearchResponse);
        }).when(session).searchReleaseGroups(anyString());
        DiscoveryJob nextJob = job(DiscoveryType.FULL);

        fullDiscoveryService.discover(nextJob, false, null);

        assertThat(taskRepository.countByJobId(nextJob.getId())).isEqualTo(2);
    }

    @Test
    void shouldResumeEachAlbumIndependentlyAfterInterruption() {
        enableLlm();
        notFoundResponses();
        DiscoveryJob previousJob = jobRepository.save(job(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.INTERRUPTED));
        saveDiscoveryHistory(previousJob, 2, 1);
        Album emptyAlbum = albumRepository.save(new Album().setArtist(artist).setName("Empty discovery"));
        Album missingAlbum = albumRepository.save(new Album().setArtist(artist).setName("Missing discovery"));
        search(List.of());
        DiscoveryJob nextJob = job(DiscoveryType.FULL);

        fullDiscoveryService.discover(nextJob, false, null);

        assertThat(taskRepository.countByJobId(nextJob.getId())).isEqualTo(2);
        assertThat(artistTasks()).hasSize(2);
        assertThat(albumTasks(album)).singleElement()
                .satisfies(task -> assertThat(task.getJob().getId()).isEqualTo(previousJob.getId()));
        assertThat(albumTasks(emptyAlbum)).singleElement()
                .satisfies(task -> assertThat(task.getJob().getId()).isEqualTo(nextJob.getId()));
        assertThat(albumTasks(missingAlbum)).singleElement()
                .satisfies(task -> assertThat(task.getJob().getId()).isEqualTo(nextJob.getId()));
    }

    @Test
    void shouldReuseSuccessfulArtistTasksDespiteLaterFailedTask() {
        enableLlm();
        notFoundResponses();
        DiscoveryJob completedJob = jobRepository.save(job(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.COMPLETE));
        saveDiscoveryHistory(completedJob, 2, 1);
        DiscoveryJob interruptedJob = jobRepository.save(job(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.INTERRUPTED));
        task(interruptedJob, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.FAILED);
        search(List.of());
        DiscoveryJob nextJob = job(DiscoveryType.FULL);

        fullDiscoveryService.discover(nextJob, false, null);

        assertThat(taskRepository.countByJobId(nextJob.getId())).isZero();
        assertThat(albumTasks(album)).hasSize(1);
    }

    @ParameterizedTest
    @CsvSource({"true,false,2", "false,true,1", "true,true,3"})
    void shouldRetryOutdatedPartsOfFullDiscovery(boolean artistUpdated, boolean albumUpdated, int expectedNewTasks) {
        enableLlm();
        notFoundResponses();
        DiscoveryJob previousJob = jobRepository.save(job(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.COMPLETE));
        saveDiscoveryHistory(previousJob, 2, 1);
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("UPDATE discovery_task SET creation_date = ?, update_date = NULL WHERE discovery_job_id = ?",
                now, previousJob.getId());
        jdbcTemplate.update("UPDATE artist SET creation_date = ?, update_date = NULL WHERE id = ?", now.minusDays(1), artist.getId());
        jdbcTemplate.update("UPDATE album SET creation_date = ?, update_date = NULL WHERE id = ?", now.minusDays(1), album.getId());
        if (artistUpdated) {
            jdbcTemplate.update("UPDATE artist SET update_date = ? WHERE id = ?", now.plusMinutes(1), artist.getId());
        }
        if (albumUpdated) {
            jdbcTemplate.update("UPDATE album SET update_date = ? WHERE id = ?", now.plusMinutes(1), album.getId());
        }
        search(List.of());
        DiscoveryJob nextJob = job(DiscoveryType.FULL);

        fullDiscoveryService.discover(nextJob, false, null);

        assertThat(taskRepository.countByJobId(nextJob.getId())).isEqualTo(expectedNewTasks);
    }

    @ParameterizedTest
    @EnumSource(value = DiscoveryTaskType.class, names = {"SPOTIFY_TOP_TRACKS", "MUSICBRAINZ_ALBUM_DATA"})
    void shouldRerunUpdatedTaskImplementationOnceDespiteCompletedResults(DiscoveryTaskType type) {
        DiscoveryJob previousJob = job(DiscoveryType.ARTIST);
        artistService.discover(previousJob, artist, false, null);
        jobRepository.save(previousJob.setStatus(DiscoveryJob.Status.COMPLETE));
        LocalDateTime implementedAt = LocalDateTime.ofInstant(type.getImplementedAt().orElseThrow(), ZoneId.systemDefault());
        jdbcTemplate.update("UPDATE discovery_task SET creation_date = ? WHERE discovery_job_id = ? AND type = ?",
                implementedAt.minusMinutes(1), previousJob.getId(), type.name());
        jdbcTemplate.update("UPDATE artist SET creation_date = ?, update_date = NULL WHERE id = ?",
                implementedAt.minusDays(1), artist.getId());
        jdbcTemplate.update("UPDATE album SET creation_date = ?, update_date = NULL WHERE id = ?",
                implementedAt.minusDays(1), album.getId());
        DiscoveryJob nextJob = job(DiscoveryType.FULL);
        List<DiscoveryProgress> progressCalls = new ArrayList<>();

        fullDiscoveryService.discover(nextJob, false, progressCalls::add);

        assertThat(taskRepository.findAll()).filteredOn(task -> task.getJob().getId().equals(nextJob.getId()))
                .singleElement().satisfies(task -> {
                    assertThat(task.getType()).isEqualTo(type);
                    assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
                });
        assertThat(progressCalls.get(1).getValue()).isEqualTo(DiscoveryProgress.Value.of(
                type == DiscoveryTaskType.SPOTIFY_TOP_TRACKS ? 1 : 2, 3));
        assertThat(progressCalls.getLast().getValue()).isEqualTo(DiscoveryProgress.Value.of(3, 3));
        DiscoveryJob followingJob = job(DiscoveryType.FULL);

        fullDiscoveryService.discover(followingJob, false, null);

        assertThat(taskRepository.countByJobId(followingJob.getId())).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = DiscoveryType.class, names = {"ARTIST", "ALBUM"})
    void shouldKeepExplicitArtistAndAlbumDiscoveriesForced(DiscoveryType type) {
        enableLlm();
        notFoundResponses();
        search(List.of());
        DiscoveryJob previousJob = job(DiscoveryType.ARTIST);
        artistService.discover(previousJob, artist, false, null);
        jobRepository.save(previousJob.setStatus(DiscoveryJob.Status.COMPLETE));
        DiscoveryJob nextJob = job(type);

        if (type == DiscoveryType.ARTIST) {
            artistService.discover(nextJob, artist, false, null);
        } else {
            albumService.discover(nextJob, album, false, null);
        }

        assertThat(taskRepository.countByJobId(nextJob.getId())).isEqualTo(type == DiscoveryType.ARTIST ? 3 : 1);
    }

    @Test
    void shouldRestoreArtistIndexFromReusedCompletedAlbumAfterInterruption() {
        DiscoveryJob previousJob = job(DiscoveryType.ARTIST);
        artistService.discover(previousJob, artist, false, null);
        ArtistDiscoveryIndex previousIndex = artistDiscoveryIndexRepository.findByArtistId(artist.getId()).orElseThrow();
        artistDiscoveryIndexRepository.deleteAll();
        jobRepository.save(previousJob.setStatus(DiscoveryJob.Status.INTERRUPTED));
        DiscoveryJob nextJob = job(DiscoveryType.FULL);

        fullDiscoveryService.discover(nextJob, false, null);

        assertThat(taskRepository.countByJobId(nextJob.getId())).isZero();
        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
            assertThat(index.getMusicBrainzArtistId()).isEqualTo(previousIndex.getMusicBrainzArtistId());
            assertThat(index.getCountryCode()).isEqualTo(previousIndex.getCountryCode());
        });
    }

    @Test
    void shouldRestoreIndexFromLaterAlbumWhenFirstAlbumResultIsNullAfterInterruption() {
        albumRepository.save(new Album().setArtist(artist).setName("Earlier demo").setYear(1980));
        DiscoveryJob previousJob = job(DiscoveryType.ARTIST);
        artistService.discover(previousJob, artist, false, null);
        artistDiscoveryIndexRepository.deleteAll();
        jobRepository.save(previousJob.setStatus(DiscoveryJob.Status.INTERRUPTED));
        clearInvocations(session);
        DiscoveryJob nextJob = job(DiscoveryType.FULL);

        fullDiscoveryService.discover(nextJob, false, null);

        assertThat(taskRepository.countByJobId(nextJob.getId())).isZero();
        verifyNoInteractions(session);
        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
            assertThat(index.getMusicBrainzArtistId()).isEqualTo("artist");
            assertThat(index.getCountryCode()).isEqualTo("US");
        });
    }

    @Test
    void shouldKeepArtistIndexWhenLaterAlbumIsInterrupted() {
        albumRepository.save(new Album().setArtist(artist).setName("Second album").setYear(2000));
        search(List.of(
                group("first-group", "First group", "Reign in Blood", List.of(credit("artist", "Slayer", "Slayer"))),
                group("second-group", "Second group", "Second album", List.of(credit("artist", "Slayer", "Slayer")))));
        AtomicInteger searchCalls = new AtomicInteger();
        doAnswer(invocation -> {
            if (searchCalls.getAndIncrement() == 1) {
                throw new DiscoveryInterruptedException();
            }
            return new MusicBrainzClient.ReleaseGroupSearchResult(groups, rawSearchResponse);
        }).when(session).searchReleaseGroups(anyString());
        DiscoveryJob interruptedJob = job(DiscoveryType.ARTIST);

        assertThatThrownBy(() -> artistService.discover(interruptedJob, artist, false, null))
                .isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(taskRepository.findAll()).filteredOn(task -> task.getAlbum() != null).extracting(DiscoveryTask::getStatus)
                .containsExactlyInAnyOrder(DiscoveryTask.Status.COMPLETE, DiscoveryTask.Status.INTERRUPTED);
        assertThat(taskRepository.findAll()).noneMatch(task -> task.getType() == DiscoveryTaskType.SPOTIFY_TOP_TRACKS);
        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
            assertThat(index.getMusicBrainzArtistId()).isEqualTo("artist");
            assertThat(index.getCountryCode()).isEqualTo("US");
        });
        jobRepository.save(interruptedJob.setStatus(DiscoveryJob.Status.INTERRUPTED));
        doReturn(new MusicBrainzClient.ReleaseGroupSearchResult(List.of(), EMPTY_SEARCH_RESPONSE)).when(session).searchReleaseGroups(anyString());
        notFoundResponses();
        DiscoveryJob nextJob = job(DiscoveryType.FULL);

        fullDiscoveryService.discover(nextJob, false, null);

        assertThat(taskRepository.countByJobId(nextJob.getId())).isEqualTo(2);
        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
            assertThat(index.getMusicBrainzArtistId()).isEqualTo("artist");
            assertThat(index.getCountryCode()).isEqualTo("US");
        });
    }

    @Test
    void shouldPublishArtistIndexBeforeReportingCompletedAlbumProgress() {
        albumRepository.save(new Album().setArtist(artist).setName("Second album").setYear(2000));
        search(List.of(
                group("first-group", "First group", "Reign in Blood", List.of(credit("artist", "Slayer", "Slayer"))),
                group("second-group", "Second group", "Second album", List.of(credit("artist", "Slayer", "Slayer")))));
        List<Long> completedAlbums = new ArrayList<>();

        artistService.discover(job(DiscoveryType.ARTIST), artist, false, progress -> {
            long complete = progress.getValue().getItemsComplete();
            if (complete > 1 && complete <= 3) {
                assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
                    assertThat(index.getMusicBrainzArtistId()).isEqualTo("artist");
                    assertThat(index.getCountryCode()).isEqualTo("US");
                });
                completedAlbums.add(complete - 1);
            }
        });

        assertThat(completedAlbums).containsExactly(1L, 2L);
    }

    @ParameterizedTest
    @EnumSource(value = DiscoveryTask.Status.class, names = {"STARTED", "FAILED", "INTERRUPTED"})
    void shouldUseCompletedAlbumResultDespiteNewerIncompleteTasks(DiscoveryTask.Status incompleteStatus) {
        DiscoveryJob previousJob = job(DiscoveryType.ALBUM);
        albumService.discover(previousJob, album, false, null);
        jobRepository.save(previousJob.setStatus(DiscoveryJob.Status.INTERRUPTED));
        DiscoveryJob incompleteJob = job(DiscoveryType.ALBUM);
        DiscoveryTask incompleteTask = task(incompleteJob, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, incompleteStatus);
        taskRepository.save(incompleteTask.setResult(JsonConverter.toJson(
                new MusicBrainzAlbumData("incomplete", "Album", List.of(), null, "incomplete-artist", List.of("DE")))));

        saveDiscoveryHistory(job(DiscoveryType.FULL), 2, -1);
        artistService.discoverIfUpdated(job(DiscoveryType.FULL), artist, List.of(album), false, null);

        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
            assertThat(index.getMusicBrainzArtistId()).isEqualTo("artist");
            assertThat(index.getCountryCode()).isEqualTo("US");
        });
    }

    @Test
    void shouldCollectCountryFromLaterAlbumWithoutReplacingFirstArtistId() {
        albumRepository.save(new Album().setArtist(artist).setName("Second album").setYear(2000));
        search(List.of(
                group("first-group", "First group", "Reign in Blood", List.of(credit("first-artist", "Slayer", "Slayer"))),
                group("second-group", "Second group", "Second album", List.of(credit("second-artist", "Slayer", "Slayer")))));
        doReturn(new MusicBrainzDto.Artist("first-artist", "Slayer", null, null)).when(session).getArtist("first-artist");
        doReturn(new MusicBrainzDto.Artist("second-artist", "Slayer", new MusicBrainzDto.Area(List.of("BE")), null))
                .when(session).getArtist("second-artist");

        artistService.discover(job(DiscoveryType.ARTIST), artist, false, null);

        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
            assertThat(index.getMusicBrainzArtistId()).isEqualTo("first-artist");
            assertThat(index.getCountryCode()).isEqualTo("BE");
        });
    }

    @Test
    void shouldCreateMusicBrainzArtistIndexFromFirstAlbum() {
        albumRepository.save(new Album().setArtist(artist).setName("Earlier album").setYear(1980));
        search(List.of(
                group("later-group", "Later group", "Reign in Blood", List.of(credit("artist", "Slayer", "Slayer"))),
                group("first-group", "First group", "Earlier album", List.of(credit("first-artist", "Slayer", "Slayer")))));
        doReturn(new MusicBrainzDto.Artist("first-artist", "Slayer",
                new MusicBrainzDto.Area(List.of("DE", "US")), null)).when(session).getArtist("first-artist");

        artistService.discover(job(DiscoveryType.ARTIST), artist, false, null);

        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
            assertThat(index.getMusicBrainzArtistId()).isEqualTo("first-artist");
            assertThat(index.getCountryCode()).isEqualTo("DE");
            assertThat(index.getSpotifyArtistId()).isNull();
            assertThat(index.getUpdateDate()).isNotNull();
        });
        assertThat(taskRepository.findAll()).hasSize(4)
                .allSatisfy(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE));
    }

    @ParameterizedTest
    @CsvSource({"updated-artist,US", "artist,DE", "updated-artist,DE"})
    void shouldUpdateChangedMusicBrainzArtistIndexValues(String artistId, String countryCode) {
        ArtistDiscoveryIndex original = artistDiscoveryIndexRepository.save(new ArtistDiscoveryIndex()
                .setArtist(artist).setSpotifyArtistId("spotify-artist")
                .setMusicBrainzArtistId("artist").setCountryCode("US"));
        search(List.of(group("group", "Group", "Reign in Blood", List.of(credit(artistId, "Slayer", "Slayer")))));
        rawArtistResponse = JsonConverter.toJson(new MusicBrainzDto.Artist(artistId, "Slayer",
                new MusicBrainzDto.Area(List.of(countryCode)), null));

        saveDiscoveryHistory(job(DiscoveryType.FULL), 2, -1);
        artistService.discoverIfUpdated(job(DiscoveryType.FULL), artist, List.of(album), false, null);

        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
            assertThat(index.getId()).isEqualTo(original.getId());
            assertThat(index.getCreationDate()).isEqualTo(original.getCreationDate());
            assertThat(index.getUpdateDate()).isNotNull();
            assertThat(index.getMusicBrainzArtistId()).isEqualTo(artistId);
            assertThat(index.getCountryCode()).isEqualTo(countryCode);
            assertThat(index.getSpotifyArtistId()).isEqualTo("spotify-artist");
        });
        assertThat(artistDiscoveryIndexRepository.count()).isOne();
    }

    @Test
    void shouldReadLatestCompletedMusicBrainzArtistDataAcrossJobs() {
        DiscoveryJob currentJob = job(DiscoveryType.ARTIST);
        DiscoveryJob otherJob = job(DiscoveryType.ALBUM);

        artistService.discover(currentJob, artist, false, null);
        rawArtistResponse = "{\"id\":\"other-job-artist\",\"area\":{\"iso-3166-1-codes\":[\"DE\"]}}";
        albumService.discover(otherJob, album, false, null);

        artistService.discoverIfUpdated(job(DiscoveryType.FULL), artist, List.of(album), false, null);

        assertThat(taskRepository.findAll()).hasSize(4);
        assertThat(taskRepository.findLatestCompletedByAlbumIdAndType(album.getId(),
                DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, org.springframework.data.domain.Limit.of(1))).get()
                .extracting(task -> task.getJob().getId()).isEqualTo(otherJob.getId());
        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
            assertThat(index.getMusicBrainzArtistId()).isEqualTo("other-job-artist");
            assertThat(index.getCountryCode()).isEqualTo("DE");
        });
    }

    @Test
    void shouldKeepMusicBrainzArtistIndexDatesWhenValuesAreUnchanged() {
        ArtistDiscoveryIndex original = artistDiscoveryIndexRepository.save(new ArtistDiscoveryIndex()
                .setArtist(artist).setMusicBrainzArtistId("artist").setCountryCode("US"));

        artistService.discover(job(DiscoveryType.ARTIST), artist, false, null);

        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
            assertThat(index.getId()).isEqualTo(original.getId());
            assertThat(index.getCreationDate()).isEqualTo(original.getCreationDate());
            assertThat(index.getUpdateDate()).isEqualTo(original.getUpdateDate());
            assertThat(index.getMusicBrainzArtistId()).isEqualTo("artist");
            assertThat(index.getCountryCode()).isEqualTo("US");
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldPreserveExistingCountryWhenMusicBrainzHasNoCountry(boolean existingIndex) {
        if (existingIndex) {
            artistDiscoveryIndexRepository.save(new ArtistDiscoveryIndex()
                    .setArtist(artist).setMusicBrainzArtistId("artist").setCountryCode("US"));
        }
        rawArtistResponse = "{\"id\":\"artist\"}";

        artistService.discover(job(DiscoveryType.ARTIST), artist, false, null);

        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
            assertThat(index.getMusicBrainzArtistId()).isEqualTo("artist");
            assertThat(index.getCountryCode()).isEqualTo(existingIndex ? "US" : null);
        });
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void shouldUseNextAlbumWhenFirstAlbumHasNoResult(boolean failed, boolean existingIndex) {
        notFoundResponses();
        if (existingIndex) {
            artistDiscoveryIndexRepository.save(new ArtistDiscoveryIndex()
                    .setArtist(artist).setMusicBrainzArtistId("previous-artist").setCountryCode("DE"));
        }
        albumRepository.save(new Album().setArtist(artist).setName("Second album").setYear(2000));
        AtomicInteger searchCalls = new AtomicInteger();
        doAnswer(invocation -> {
            if (searchCalls.getAndIncrement() == 0) {
                if (failed) {
                    throw new IllegalStateException("MusicBrainz unavailable");
                }
                return new MusicBrainzClient.ReleaseGroupSearchResult(List.of(), EMPTY_SEARCH_RESPONSE);
            }
            List<MusicBrainzDto.ReleaseGroup> candidates = List.of(
                    group("first-group", "First group", "Reign in Blood", List.of(credit("artist", "Slayer", "Slayer"))),
                    group("second-group", "Second group", "Second album", List.of(credit("artist", "Slayer", "Slayer"))));
            return new MusicBrainzClient.ReleaseGroupSearchResult(candidates,
                    JsonConverter.toJson(new MusicBrainzDto.ReleaseGroupSearch(candidates.size(), 0, candidates)));
        }).when(session).searchReleaseGroups(anyString());

        artistService.discover(job(DiscoveryType.ARTIST), artist, false, null);

        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
            assertThat(index.getMusicBrainzArtistId()).isEqualTo("artist");
            assertThat(index.getCountryCode()).isEqualTo("US");
            assertThat(index.getUpdateDate()).isNotNull();
        });
        assertThat(taskRepository.findAll()).hasSize(4);
        verify(session).getArtist("artist");
    }

    @Test
    void shouldSkipMusicBrainzArtistIndexUpdateWhenArtistHasNoAlbums() {
        albumRepository.deleteById(album.getId());
        artistDiscoveryIndexRepository.save(new ArtistDiscoveryIndex()
                .setArtist(artist).setMusicBrainzArtistId("previous-artist").setCountryCode("DE"));

        artistService.discover(job(DiscoveryType.ARTIST), artist, false, null);

        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
            assertThat(index.getMusicBrainzArtistId()).isEqualTo("previous-artist");
            assertThat(index.getCountryCode()).isEqualTo("DE");
            assertThat(index.getUpdateDate()).isNull();
        });
        assertThat(taskRepository.findAll()).hasSize(2)
                .allSatisfy(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldFindBillyNovikOnlyAfterExistingAttemptsAndCacheAlternativeSearch(boolean cacheEnabled) {
        enableLlm();
        artistRepository.save(artist.setName("Billy’s Band"));
        albumRepository.save(album.setName("Песни Егора Летова").setYear(2018));
        Song track = alternativeTrack("Билли Новик");
        String originalQuery = "release:\"Песни Егора Летова\" AND artistname:\"Billy’s Band\"";
        String normalizedQuery = "artistname:\"Billy’s Band\" AND release:\"Песни Егора\"";
        String alternativeQuery = "release:\"Песни Егора Летова\" AND (artistname:\"Билли Новик\" OR artist:\"Билли Новик\")";
        alternativeSearches(Map.of(alternativeQuery, List.of(group("group", album.getName(), album.getName(),
                List.of(credit("novik", "Билли Новик", null))))));
        rawArtistResponse = "{\"id\":\"novik\",\"country\":\"RU\"}";
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            String system = prompt.getInstructions().getFirst().getText();
            if (system.startsWith("You normalize untrusted album-title text")) {
                return response("{\"searchTitle\":\"Песни Егора Летова\",\"searchFragments\":[\"Песни Егора\"],\"alternativeTitles\":[]}");
            }
            if (system.startsWith("Choose alternative album artist names")) {
                verify(session, atLeastOnce()).searchReleaseGroups(normalizedQuery);
                assertThat(prompt.getInstructions().get(1).getText()).contains("Billy’s Band", "Билли Новик", track.getName());
                return response("{\"artistNames\":[\"Билли Новик\"]}");
            }
            assertThat(system).contains("organizational grouping");
            assertThat(prompt.getInstructions().get(1).getText()).contains("Alternative artist search names", "Billy’s Band", "Билли Новик");
            return response("{\"id\":\"group\"}");
        });

        albumService.discover(job(DiscoveryType.ALBUM), album, cacheEnabled, null);
        albumService.discover(job(DiscoveryType.ALBUM), album, cacheEnabled, null);

        verify(model, times(cacheEnabled ? 3 : 6)).call(any(Prompt.class));
        songRepository.save(track.setAlbumArtistName("Updated album artist tag"));
        albumService.discover(job(DiscoveryType.ALBUM), album, cacheEnabled, null);

        verify(model, times(cacheEnabled ? 5 : 9)).call(any(Prompt.class));
        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(session, times(9)).searchReleaseGroups(queries.capture());
        assertThat(queries.getAllValues()).containsExactly(originalQuery, normalizedQuery, alternativeQuery,
                originalQuery, normalizedQuery, alternativeQuery, originalQuery, normalizedQuery, alternativeQuery);
        verify(session, times(3)).getArtist("novik");
        assertThat(artistRepository.findById(artist.getId()).orElseThrow().getName()).isEqualTo("Billy’s Band");
        assertThat(taskRepository.findAll()).hasSize(3).allSatisfy(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            MusicBrainzAlbumData result = JsonConverter.fromJson(task.getResult(), MusicBrainzAlbumData.class);
            assertThat(result.releaseGroupId()).isEqualTo("group");
            assertThat(result.artistId()).isEqualTo("novik");
            assertThat(result.country()).containsExactly("RU");
            assertThat(JsonConverter.fromJson(task.getRawRequest(), Object[].class)).hasSize(7);
            assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).hasSize(7);
        });
        assertThat(cacheRepository.findAll()).filteredOn(entry -> entry.getKey().startsWith("MUSICBRAINZ_ALBUM_ARTIST_SEARCH:"))
                .hasSize(cacheEnabled ? 2 : 0);
    }

    private Song alternativeTrack(String name) {
        Genre genre = genreRepository.save(new Genre().setName("Genre"));
        return songRepository.save(song().setPath("alternative-" + songRepository.count()).setArtistName(name)
                .setAlbumArtistName(artist.getName()).setAlbum(album).setGenre(genre));
    }

    private void alternativeSearches(Map<String, List<MusicBrainzDto.ReleaseGroup>> results) {
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            String query = invocation.getArgument(0);
            List<MusicBrainzDto.ReleaseGroup> candidates = results.getOrDefault(query, List.of());
            String raw = JsonConverter.toJson(new MusicBrainzDto.ReleaseGroupSearch(candidates.size(), 0, candidates));
            String url = "https://musicbrainz.org/ws/2/release-group?query=" + URLEncoder.encode(query, UTF_8);
            exchangeObserver.onRequest(url);
            exchangeObserver.onResponse(url, raw);
            return new MusicBrainzClient.ReleaseGroupSearchResult(candidates, raw);
        }).when(session).searchReleaseGroups(anyString());
    }

    @ParameterizedTest
    @CsvSource({"Mortal Kombat,Mortal Kombat,1995", "Mortician,Tribute to Mortician,2007"})
    void shouldFindVariousArtistsAlbumsWithExistingSelectionPrompt(String libraryArtist, String title, int year) {
        enableLlm();
        artistRepository.save(artist.setName(libraryArtist));
        albumRepository.save(album.setName(title).setYear(year));
        alternativeTrack("First Performer");
        alternativeTrack("Second Performer");
        String alternativeQuery = "release:\"" + title + "\" AND artistname:\"Various Artists\"";
        MusicBrainzDto.ReleaseGroup correct = new MusicBrainzDto.ReleaseGroup("group", title, "Album", List.of(),
                Integer.toString(year), List.of(credit("various", "Various Artists", null)),
                List.of(new MusicBrainzDto.Release("release", title)));
        alternativeSearches(Map.of(alternativeQuery, List.of(correct)));
        rawArtistResponse = "{\"id\":\"various\"}";
        alternativeArtistResponses(List.of("Various Artists"), "group");

        albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        MusicBrainzAlbumData result = JsonConverter.fromJson(onlyTask().getResult(), MusicBrainzAlbumData.class);
        assertThat(result.releaseGroupId()).isEqualTo("group");
        assertThat(result.artistId()).isEqualTo("various");
        assertThat(result.secondaryTypes()).isEmpty();
        assertThat(result.country()).isEmpty();
        verify(session).searchReleaseGroups(alternativeQuery);
        ArgumentCaptor<Prompt> prompts = ArgumentCaptor.forClass(Prompt.class);
        verify(model, times(3)).call(prompts.capture());
        assertThat(prompts.getAllValues().getLast().getInstructions().getFirst().getText())
                .startsWith("Choose the MusicBrainz release group")
                .contains("does not require the Compilation secondary type");
    }

    private void alternativeArtistResponses(List<String> names, String groupId) {
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            String system = prompt.getInstructions().getFirst().getText();
            if (system.startsWith("You normalize untrusted album-title text")) {
                String title = (String) JsonConverter.fromJson(prompt.getInstructions().get(1).getText(), Map.class).get("albumTitle");
                return response(JsonConverter.toJson(Map.of("searchTitle", title, "searchFragments", List.of(), "alternativeTitles", List.of())));
            }
            if (system.startsWith("Choose alternative album artist names")) {
                return response(JsonConverter.toJson(Map.of("artistNames", names)));
            }
            return response(prompt.getInstructions().get(1).getText().contains("Alternative artist search names:")
                    ? JsonConverter.toJson(Map.of("id", groupId)) : "{\"id\":null}");
        });
    }

    @Test
    void shouldSearchAllAlternativeNamesAndMergeCandidatesBeforeSelection() {
        enableLlm();
        alternativeTrack("First Artist");
        alternativeTrack("Second Artist");
        MusicBrainzDto.ReleaseGroup misleading = group("wrong", album.getName(), album.getName(), List.of(credit("wrong-artist", "First Artist", null)));
        MusicBrainzDto.ReleaseGroup correct = group("group", album.getName(), "Reign in Blood (expanded)", List.of(credit("artist", "Second Artist", null)));
        String firstQuery = "release:\"Reign in Blood\" AND (artistname:\"First Artist\" OR artist:\"First Artist\")";
        String secondQuery = "release:\"Reign in Blood\" AND (artistname:\"Second Artist\" OR artist:\"Second Artist\")";
        alternativeSearches(Map.of(firstQuery, List.of(misleading, correct), secondQuery, List.of(correct)));
        alternativeArtistResponses(List.of("First Artist", "first artist", "Second Artist"), "group");

        albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(JsonConverter.fromJson(onlyTask().getResult(), MusicBrainzAlbumData.class).releaseGroupId()).isEqualTo("group");
        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(session, times(3)).searchReleaseGroups(queries.capture());
        assertThat(queries.getAllValues()).containsExactly("release:\"Reign in Blood\" AND artistname:\"Slayer\"", firstQuery, secondQuery);
        ArgumentCaptor<Prompt> prompts = ArgumentCaptor.forClass(Prompt.class);
        verify(model, times(3)).call(prompts.capture());
        String context = prompts.getAllValues().getLast().getInstructions().get(1).getText();
        String responseJson = context.split("\nMusicBrainz search response: ", 2)[1].split("\n", 2)[0];
        Map<String, Object> data = JsonConverter.mapFromJson(responseJson, String.class, Object.class);
        assertThat((List<?>) data.get("release-groups")).hasSize(2);
        assertThat(context).contains("First Artist", "Second Artist");
    }

    @ParameterizedTest
    @MethodSource("missingOrMatchingTrackArtists")
    void shouldSkipAlternativeAnalysisWithoutDifferentNonBlankTrackArtist(String trackArtist) {
        enableLlm();
        alternativeTrack(trackArtist);
        alternativeSearches(Map.of());

        albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(onlyTask().getResult()).isEqualTo("null");
        verify(model, times(1)).call(any(Prompt.class));
        verify(session, times(1)).searchReleaseGroups(anyString());
    }

    private static Stream<String> missingOrMatchingTrackArtists() {
        return Stream.of(null, "", "  ", "Slayer", " slAyEr ");
    }

    @Test
    void shouldSupplyAlternativeNamesAndTracksWhenChoosingAmongCreditedArtists() {
        enableLlm();
        alternativeTrack("First Artist");
        alternativeTrack("Second Artist");
        MusicBrainzDto.ReleaseGroup candidate = group("group", album.getName(), album.getName(),
                List.of(credit("first", "First Artist", null), credit("second", "Second Artist", null)));
        alternativeSearches(Map.of(
                "release:\"Reign in Blood\" AND (artistname:\"First Artist\" OR artist:\"First Artist\")", List.of(candidate),
                "release:\"Reign in Blood\" AND (artistname:\"Second Artist\" OR artist:\"Second Artist\")", List.of(candidate)));
        rawArtistResponse = "{\"id\":\"second\",\"country\":\"CA\"}";
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            String system = prompt.getInstructions().getFirst().getText();
            if (system.startsWith("You normalize untrusted album-title text")) {
                return response("{\"searchTitle\":\"Reign in Blood\",\"searchFragments\":[],\"alternativeTitles\":[]}");
            }
            if (system.startsWith("Choose alternative album artist names")) {
                return response("{\"artistNames\":[\"First Artist\",\"Second Artist\"]}");
            }
            if (system.startsWith("Choose the MusicBrainz artist")) {
                assertThat(system).contains("organizational grouping");
                assertThat(prompt.getInstructions().get(1).getText()).contains("Alternative artist search names:",
                        "First Artist", "Second Artist", "Library album track count: 2", "Library album tracks:", "Slayer");
                return response("{\"id\":\"second\"}");
            }
            return response("{\"id\":\"group\"}");
        });

        albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        MusicBrainzAlbumData result = JsonConverter.fromJson(onlyTask().getResult(), MusicBrainzAlbumData.class);
        assertThat(result.releaseGroupId()).isEqualTo("group");
        assertThat(result.artistId()).isEqualTo("second");
        assertThat(result.country()).containsExactly("CA");
        verify(model, times(4)).call(any(Prompt.class));
        verify(session, times(3)).searchReleaseGroups(anyString());
        verify(session).getArtist("second");
    }

    @Test
    void shouldPreserveSuccessfulSearchEvenWhenTrackArtistsDiffer() {
        alternativeTrack("Alternative Artist");

        albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

        assertThat(onlyTask().getResult()).isNotEqualTo("null");
        verify(model, never()).call(any(Prompt.class));
        verify(session, times(1)).searchReleaseGroups(anyString());
    }

    @Test
    void shouldPreserveSuccessfulTitleNormalizationWhenTrackArtistsDiffer() {
        enableLlm();
        alternativeTrack("Alternative Artist");
        searchAfterEmpty(groups);
        when(model.call(any(Prompt.class))).thenReturn(response(
                "{\"searchTitle\":\"Reign in Blood\",\"searchFragments\":[\"Reign\"],\"alternativeTitles\":[]}"));

        albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

        assertThat(onlyTask().getResult()).isNotEqualTo("null");
        verify(model, times(1)).call(any(Prompt.class));
        verify(session, times(2)).searchReleaseGroups(anyString());
    }

    @Test
    void shouldNotSearchAgainWhenAlbumWasFoundButArtistSelectionFailed() {
        enableLlm();
        alternativeTrack("Alternative Artist");
        search(List.of(group("group", album.getName(), album.getName(),
                List.of(credit("first", "First", null), credit("second", "Second", null)))));

        albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(onlyTask().getResult()).isEqualTo("null");
        verify(model, times(1)).call(any(Prompt.class));
        verify(session, times(1)).searchReleaseGroups(anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReuseOrLazilyPerformNormalizationForAlternativeArtist(boolean normalizedBeforeFallback) {
        enableLlm();
        albumRepository.save(album.setName("Reign in Blood (Deluxe)"));
        alternativeTrack("Alternative Artist");
        String original = "release:\"Reign in Blood \\(Deluxe\\)\" AND artistname:\"Slayer\"";
        String normalized = "artistname:\"Slayer\" AND release:\"Reign in Blood\"";
        String alternative = "release:\"Reign in Blood \\(Deluxe\\)\" AND (artistname:\"Alternative Artist\" OR artist:\"Alternative Artist\")";
        String normalizedAlternative = "(artistname:\"Alternative Artist\" OR artist:\"Alternative Artist\") AND release:\"Reign in Blood\"";
        Map<String, List<MusicBrainzDto.ReleaseGroup>> results = new LinkedHashMap<>();
        if (!normalizedBeforeFallback) {
            results.put(original, List.of(group("rejected", "Unrelated", "Unrelated", List.of(credit("wrong", "Slayer", null)))));
        }
        results.put(normalizedAlternative, List.of(group("group", "Reign in Blood", "Reign in Blood", List.of(credit("artist", "Alternative Artist", null)))));
        alternativeSearches(results);
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            String system = prompt.getInstructions().getFirst().getText();
            if (system.startsWith("You normalize untrusted album-title text")) {
                return response("{\"searchTitle\":\"Reign in Blood\",\"searchFragments\":[],\"alternativeTitles\":[]}");
            }
            if (system.startsWith("Choose alternative album artist names")) {
                return response("{\"artistNames\":[\"Alternative Artist\"]}");
            }
            return response(prompt.getInstructions().get(1).getText().contains("Alternative artist search names:")
                    ? "{\"id\":\"group\"}" : "{\"id\":null}");
        });

        albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(JsonConverter.fromJson(onlyTask().getResult(), MusicBrainzAlbumData.class).releaseGroupId()).isEqualTo("group");
        ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
        verify(session, times(normalizedBeforeFallback ? 4 : 3)).searchReleaseGroups(queries.capture());
        assertThat(queries.getAllValues()).containsExactlyElementsOf(normalizedBeforeFallback
                ? List.of(original, normalized, alternative, normalizedAlternative) : List.of(original, alternative, normalizedAlternative));
        ArgumentCaptor<Prompt> prompts = ArgumentCaptor.forClass(Prompt.class);
        verify(model, times(normalizedBeforeFallback ? 3 : 4)).call(prompts.capture());
        assertThat(prompts.getAllValues()).filteredOn(prompt -> prompt.getInstructions().getFirst().getText()
                .startsWith("You normalize untrusted album-title text")).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not JSON", "{}", "{\"artistNames\":null}", "{\"artistNames\":[12]}",
            "{\"artistNames\":[\"Invented Artist\"]}", "{\"artistNames\":[\"Slayer\"]}", "{\"artistNames\":[\" \"]}"})
    void shouldValidateAlternativeArtistNamesAndCacheOnlyAcceptedResponse(String rejected) {
        enableLlm();
        alternativeTrack("Alternative Artist");
        alternativeSearches(Map.of());
        when(model.call(any(Prompt.class))).thenReturn(response(
                        "{\"searchTitle\":\"Reign in Blood\",\"searchFragments\":[],\"alternativeTitles\":[]}"),
                response(rejected), response("{\"artistNames\":[]}"));

        albumService.discover(job(DiscoveryType.ALBUM), album, true, null);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(onlyTask().getResult()).isEqualTo("null");
        verify(session, times(1)).searchReleaseGroups(anyString());
        ArgumentCaptor<Prompt> prompts = ArgumentCaptor.forClass(Prompt.class);
        verify(model, times(3)).call(prompts.capture());
        assertThat(prompts.getAllValues().getLast().getInstructions()).anySatisfy(message ->
                assertThat(message.getText()).contains("Your previous response was invalid", "exactly artistNames"));
        assertThat(cacheRepository.findAll()).filteredOn(entry -> entry.getKey().startsWith("MUSICBRAINZ_ALBUM_ARTIST_SEARCH:"))
                .singleElement().satisfies(entry -> assertThat(entry.getValue()).contains("artistNames"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"analysis", "search"})
    void shouldCancelAlternativeArtistAnalysisAndSearch(String phase) {
        enableLlm();
        alternativeTrack("Alternative Artist");
        alternativeSearches(Map.of());
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            if (prompt.getInstructions().getFirst().getText().startsWith("You normalize untrusted album-title text")) {
                return response("{\"searchTitle\":\"Reign in Blood\",\"searchFragments\":[],\"alternativeTitles\":[]}");
            }
            if (phase.equals("analysis")) {
                jobSynchronizer.cancelDiscovery();
            }
            return response("{\"artistNames\":[\"Alternative Artist\"]}");
        });
        if (phase.equals("search")) {
            doAnswer(invocation -> {
                jobSynchronizer.cancelDiscovery();
                throw new MusicBrainzClient.CancelledException();
            }).when(session).searchReleaseGroups("release:\"Reign in Blood\" AND (artistname:\"Alternative Artist\" OR artist:\"Alternative Artist\")");
        }

        assertThatThrownBy(() -> albumService.discover(job(DiscoveryType.ALBUM), album, true, null))
                .isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.INTERRUPTED);
        verify(model, times(2)).call(any(Prompt.class));
        verify(session, times(phase.equals("search") ? 2 : 1)).searchReleaseGroups(anyString());
        verify(session, never()).getArtist(anyString());
        verify(session).cancel();
        verify(session).close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"initial", "normalized", "alternative", "normalized-alternative"})
    void shouldStopOversizedMusicBrainzResponsesWithoutFurtherLlmCalls(String phase) {
        enableLlm();
        alternativeTrack("Alternative Artist");
        MusicBrainzDto.ReleaseGroup candidate = group("group", album.getName(), album.getName(), List.of(credit("artist", "Slayer", null)));
        String originalQuery = "release:\"Reign in Blood\" AND artistname:\"Slayer\"";
        String normalizedQuery = "artistname:\"Slayer\" AND release:\"Reign\"";
        String alternativeQuery = "release:\"Reign in Blood\" AND (artistname:\"Alternative Artist\" OR artist:\"Alternative Artist\")";
        String normalizedAlternativeQuery = "(artistname:\"Alternative Artist\" OR artist:\"Alternative Artist\") AND release:\"Reign\"";
        String targetQuery = switch (phase) {
            case "initial" -> originalQuery;
            case "normalized" -> normalizedQuery;
            case "alternative" -> alternativeQuery;
            case "normalized-alternative" -> normalizedAlternativeQuery;
            default -> throw new IllegalArgumentException(phase);
        };
        alternativeSearches(Map.of(targetQuery, List.of(candidate)));
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            if (prompt.getInstructions().getFirst().getText().startsWith("You normalize untrusted album-title text")) {
                return response(JsonConverter.toJson(Map.of("searchTitle", "Reign in Blood", "searchFragments",
                        phase.equals("normalized") || phase.equals("normalized-alternative") ? List.of("Reign") : List.of(),
                        "alternativeTitles", List.of())));
            }
            return response("{\"artistNames\":[\"Alternative Artist\"]}");
        });
        int originalLimit = (int) ReflectionTestUtils.getField(musicBrainzAlbumDataService, "maxSearchResponseBytes");
        int limit = JsonConverter.toJson(Map.of("release-groups", List.of(candidate))).getBytes(UTF_8).length - 1;
        ReflectionTestUtils.setField(musicBrainzAlbumDataService, "maxSearchResponseBytes", limit);
        try {
            albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

            assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(onlyTask().getResult()).isEqualTo("null");
            verify(model, times(phase.equals("initial") ? 0 : phase.equals("normalized") ? 1 : 2)).call(any(Prompt.class));
            verify(session, times(phase.equals("initial") ? 1 : phase.equals("normalized-alternative") ? 4 : 2))
                    .searchReleaseGroups(anyString());
            verify(session, never()).getArtist(anyString());
            assertThat(logRepository.findAll()).filteredOn(log -> log.getText().startsWith("Skipping MusicBrainz discovery"))
                    .singleElement().satisfies(log -> {
                        assertThat(log.getLevel()).isEqualTo(LogMessage.Level.WARN);
                        assertThat(log.getText()).contains("Slayer", "Reign in Blood", "exceeds the limit", Integer.toString(limit));
                    });
        } finally {
            ReflectionTestUtils.setField(musicBrainzAlbumDataService, "maxSearchResponseBytes", originalLimit);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldMeasureResponseLimitInUtf8BytesAndAllowExactBoundary(boolean exactByteLimit) {
        search(List.of(group("group", "Русское название", album.getName(), List.of(credit("artist", "Slayer", null)))));
        int bytes = rawSearchResponse.getBytes(UTF_8).length;
        assertThat(bytes).isGreaterThan(rawSearchResponse.length());
        int originalLimit = (int) ReflectionTestUtils.getField(musicBrainzAlbumDataService, "maxSearchResponseBytes");
        ReflectionTestUtils.setField(musicBrainzAlbumDataService, "maxSearchResponseBytes", exactByteLimit ? bytes : rawSearchResponse.length());
        try {
            albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

            assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(onlyTask().getResult().equals("null")).isEqualTo(!exactByteLimit);
            verify(model, never()).call(any(Prompt.class));
            verify(session, times(exactByteLimit ? 1 : 0)).getArtist(anyString());
        } finally {
            ReflectionTestUtils.setField(musicBrainzAlbumDataService, "maxSearchResponseBytes", originalLimit);
        }
    }

    @Test
    void shouldApplyResponseLimitToMergedAlternativeResults() {
        enableLlm();
        alternativeTrack("First Artist");
        alternativeTrack("Second Artist");
        MusicBrainzDto.ReleaseGroup first = group("first", album.getName(), album.getName(), List.of(credit("first-artist", "First Artist", null)));
        MusicBrainzDto.ReleaseGroup second = group("second", album.getName(), album.getName(), List.of(credit("second-artist", "Second Artist", null)));
        alternativeSearches(Map.of(
                "release:\"Reign in Blood\" AND (artistname:\"First Artist\" OR artist:\"First Artist\")", List.of(first),
                "release:\"Reign in Blood\" AND (artistname:\"Second Artist\" OR artist:\"Second Artist\")", List.of(second)));
        alternativeArtistResponses(List.of("First Artist", "Second Artist"), "first");
        int limit = Math.max(JsonConverter.toJson(new MusicBrainzDto.ReleaseGroupSearch(1, 0, List.of(first))).getBytes(UTF_8).length,
                JsonConverter.toJson(new MusicBrainzDto.ReleaseGroupSearch(1, 0, List.of(second))).getBytes(UTF_8).length);
        int originalLimit = (int) ReflectionTestUtils.getField(musicBrainzAlbumDataService, "maxSearchResponseBytes");
        ReflectionTestUtils.setField(musicBrainzAlbumDataService, "maxSearchResponseBytes", limit);
        try {
            albumService.discover(job(DiscoveryType.ALBUM), album, false, null);

            assertThat(onlyTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(onlyTask().getResult()).isEqualTo("null");
            verify(model, times(2)).call(any(Prompt.class));
            verify(session, times(3)).searchReleaseGroups(anyString());
            verify(session, never()).getArtist(anyString());
        } finally {
            ReflectionTestUtils.setField(musicBrainzAlbumDataService, "maxSearchResponseBytes", originalLimit);
        }
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
            return new MusicBrainzClient.ReleaseGroupSearchResult(groups, rawSearchResponse);
        }).when(session).searchReleaseGroups(anyString());
    }

    private void searchAfterEmpty(List<MusicBrainzDto.ReleaseGroup> candidates) {
        search(candidates);
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            boolean initial = calls.getAndIncrement() % 2 == 0;
            String url = initial ? SEARCH_URL : NORMALIZED_SEARCH_URL;
            exchangeObserver.onRequest(url);
            exchangeObserver.onResponse(url, initial ? EMPTY_SEARCH_RESPONSE : rawSearchResponse);
            return new MusicBrainzClient.ReleaseGroupSearchResult(initial ? List.of() : groups, initial ? EMPTY_SEARCH_RESPONSE : rawSearchResponse);
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
        return new MusicBrainzDto.ArtistCredit(creditedName, " / ", new MusicBrainzDto.Artist(id, name, null, null));
    }

    private void saveDiscoveryHistory(DiscoveryJob job, int artistTasks, int albumTasks) {
        if (artistTasks >= 0) {
            List<DiscoveryTask> tasks = new ArrayList<>();
            if (artistTasks >= 1) {
                tasks.add(task(job, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.COMPLETE));
            }
            if (artistTasks >= 2) {
                tasks.add(task(job, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, DiscoveryTask.Status.COMPLETE));
            }
        }
        if (albumTasks >= 0) {
            List<DiscoveryTask> tasks = albumTasks == 0 ? List.of()
                    : List.of(task(job, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, DiscoveryTask.Status.COMPLETE));
        }
    }

    private DiscoveryTask task(DiscoveryJob job, DiscoveryTaskType type, DiscoveryTask.Status status) {
        DiscoveryTask task = new DiscoveryTask().setJob(job).setType(type).setStatus(status);
        if (type == DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA) {
            task.setAlbum(album);
        } else {
            task.setArtist(artist);
        }
        return taskRepository.save(task);
    }

    private DiscoveryJob job(DiscoveryType type) {
        return jobRepository.save(new DiscoveryJob().setType(type).setStatus(DiscoveryJob.Status.STARTED));
    }

    private List<DiscoveryTask> artistTasks() {
        return taskRepository.findAll().stream().filter(task -> task.getArtist() != null).toList();
    }

    private List<DiscoveryTask> albumTasks(Album target) {
        return taskRepository.findAll().stream().filter(task -> task.getAlbum() != null &&
                task.getAlbum().getId().equals(target.getId())).toList();
    }

    private DiscoveryTask onlyTask() {
        return taskRepository.findAll().getFirst();
    }

    private void enableLlm() {
        configService.update(new ConfigSetUpdateCommand().setDiscoveryEnabled(true).setLlmUrl("http://localhost:11434/v1").setLlmModel("test"));
    }

    private void notFoundResponses() {
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            if (prompt.getInstructions().getFirst().getText().startsWith("You normalize untrusted album-title text")) {
                String title = (String) JsonConverter.fromJson(prompt.getInstructions().get(1).getText(), Map.class).get("albumTitle");
                return response(JsonConverter.toJson(Map.of("searchTitle", title, "searchFragments", List.of(), "alternativeTitles", List.of())));
            }
            if (prompt.getInstructions().getFirst().getText().startsWith("Choose the MusicBrainz")) {
                return response("{\"id\":null}");
            }
            if (prompt.getInstructions().getFirst().getText().startsWith("Choose alternative album artist names")) {
                return response("{\"artistNames\":[]}");
            }
            return response("{\"status\":\"NOT_FOUND\"}");
        });
    }

    private ChatResponse response(String value) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(value))));
    }
}
