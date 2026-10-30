package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData.ReleaseType;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.Song;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.api.library.service.LibraryService;
import net.dorokhov.pony2.api.llm.service.DiscoveryCacheService;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.ShutdownService;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.discovery.task.SpotifyTopTracksService;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Limit;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.dorokhov.pony2.api.llm.domain.DiscoveryCacheRegion.SPOTIFY;
import static net.dorokhov.pony2.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotifyTopTracksServiceTest {

    private static final String ALBUM_RESPONSE = """
            {"albumMatches":[
                {"spotifyAlbumId":"spotify-a","databaseAlbumId":"album-a"},
                {"spotifyAlbumId":"spotify-b","databaseAlbumId":"album-b"},
                {"spotifyAlbumId":"spotify-missing","databaseAlbumId":null}
            ]}
            """;
    private static final String TRACK_RESPONSE = """
            {"trackMatches":[
                {"databaseTrackId":"song-a"},
                {"databaseTrackId":"song-b"},
                {"databaseTrackId":null}
            ]}
            """;
    private static final String NO_TRACK_MATCHES = """
            {"trackMatches":[{"databaseTrackId":null},{"databaseTrackId":null},{"databaseTrackId":null}]}
            """;

    @Mock private ChatModel model;
    @Mock private DiscoveryCacheService cacheService;
    @Mock private ArtistRepository artistRepository;
    @Mock private DiscoveryTaskRepository taskRepository;
    @Mock private LibraryService libraryService;
    @Mock private LogService logService;

    private final LibraryJobSynchronizer jobSynchronizer = new LibraryJobSynchronizer();
    private final ShutdownService shutdownService = new ShutdownService();
    private final List<String> responses = new ArrayList<>();
    private final List<Prompt> prompts = new ArrayList<>();
    private final Map<String, String> cache = new HashMap<>();
    private LibraryJobSynchronizer.LibraryJobRegistration jobRegistration;
    private SpotifyTopTracksService service;
    private DiscoveryTask previousTask;
    private Artist artist;

    @BeforeEach
    void setUp() throws Exception {
        jobRegistration = jobSynchronizer.registerDiscoveryJob();
        artist = new Artist().setId("artist").setName("Artist");
        Album first = new Album().setId("album-a").setName("First album").setYear(2000).setArtist(artist);
        Album second = new Album().setId("album-b").setName("Second album").setYear(2001).setArtist(artist);
        first.setSongs(List.of(new Song().setId("song-a").setName("Shared title").setAlbum(first)));
        second.setSongs(List.of(new Song().setId("song-b").setName("Shared title").setAlbum(second)));
        artist.setAlbums(List.of(second, first));
        SpotifyArtistData data = new SpotifyArtistData(
                new SpotifyArtistData.SpotifyArtist("spotify-artist", "Artist", "artist-url"),
                new SpotifyArtistData.MatchedAlbum("First album", "First album", "spotify-a", "album-url"),
                List.of(topTrack("first", "spotify-a"), topTrack("second", "spotify-b"), topTrack("missing", "spotify-missing")),
                null, null, null, null, null);
        previousTask = new DiscoveryTask().setId("previous").setStatus(DiscoveryTask.Status.COMPLETE).setResult(JsonConverter.toJson(data));
        lenient().when(taskRepository.findLatestCompletedByArtistIdAndType("artist", DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .thenReturn(Optional.of(previousTask));
        lenient().when(artistRepository.findById("artist")).thenReturn(Optional.of(artist));
        lenient().when(libraryService.getAlbumDiscoveryData(anyString())).thenReturn(Optional.empty());
        lenient().when(model.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
        lenient().when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            prompts.add(prompt);
            if (((OpenAiChatOptions) prompt.getOptions()).getResponseFormat().getType() == ResponseFormat.Type.TEXT) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("Collected matching data"))));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage(responses.removeFirst()))));
        });
        lenient().when(cacheService.get(eq(SPOTIFY), anyString(), eq(1)))
                .thenAnswer(invocation -> Optional.ofNullable(cache.get(invocation.getArgument(1))));
        lenient().when(cacheService.put(eq(SPOTIFY), anyString(), eq(1), anyString())).thenAnswer(invocation -> {
            cache.put(invocation.getArgument(1), invocation.getArgument(3));
            return null;
        });
        ChatClient client = ChatClient.builder(model).build();
        DiscoveryChatClient discoveryClient = new DiscoveryChatClient((customizer, toolsEnabled) -> client,
                jobSynchronizer, new DiscoveryAdvisor(shutdownService, jobSynchronizer));
        DiscoveryTaskLlmExecutor executor = new DiscoveryTaskLlmExecutor(discoveryClient, cacheService,
                taskRepository, logService, jobSynchronizer, shutdownService, transactionManager(), 1, 1, Duration.ofMinutes(10));
        service = new SpotifyTopTracksService(executor, artistRepository, taskRepository, libraryService, logService,
                shutdownService, jobSynchronizer, transactionManager(),
                new ClassPathResource("prompts/spotify-top-tracks-albums.txt"),
                new ClassPathResource("prompts/spotify-top-tracks.txt"),
                new ClassPathResource("prompts/spotify-top-tracks-verification.txt"),
                new ClassPathResource("prompts/spotify-top-tracks-fallback.txt"),
                new ClassPathResource("prompts/spotify-top-tracks-fallback-verification.txt"));
        responses.addAll(List.of(ALBUM_RESPONSE, TRACK_RESPONSE));
    }

    @AfterEach
    void tearDown() {
        jobRegistration.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldMatchTracksWithinTheirAlbumsAndPreserveNullPositions(boolean cacheEnabled) {
        DiscoveryTask task = task();

        assertThat(service.discover(task, cacheEnabled)).containsExactly("song-a", "song-b", null);

        assertThat(task.getRawResults()).containsExactly(JsonConverter.fromJsonOrString(ALBUM_RESPONSE), JsonConverter.fromJsonOrString(TRACK_RESPONSE));
        assertThat(prompts).hasSize(4);
        Map<?, ?> albumSchema = JsonConverter.fromJson(((OpenAiChatOptions) prompts.get(1).getOptions()).getResponseFormat().getJsonSchema(), Map.class);
        assertThat(albumSchema.get("type")).isEqualTo("object");
        assertThat(albumSchema.get("required")).isEqualTo(List.of("albumMatches"));
        Map<?, ?> albumMatchesSchema = (Map<?, ?>) ((Map<?, ?>) albumSchema.get("properties")).get("albumMatches");
        Map<?, ?> albumItemSchema = (Map<?, ?>) albumMatchesSchema.get("items");
        assertThat(albumItemSchema.get("required")).isEqualTo(List.of("databaseAlbumId", "spotifyAlbumId"));
        Map<?, ?> albumIdSchema = (Map<?, ?>) ((Map<?, ?>) albumItemSchema.get("properties")).get("databaseAlbumId");
        assertThat(albumIdSchema.get("type")).isEqualTo(List.of("string", "null"));
        Map<?, ?> trackSchema = JsonConverter.fromJson(((OpenAiChatOptions) prompts.getLast().getOptions()).getResponseFormat().getJsonSchema(), Map.class);
        assertThat(trackSchema.get("type")).isEqualTo("object");
        assertThat(trackSchema.get("required")).isEqualTo(List.of("trackMatches"));
        Map<?, ?> trackMatchesSchema = (Map<?, ?>) ((Map<?, ?>) trackSchema.get("properties")).get("trackMatches");
        Map<?, ?> trackItemSchema = (Map<?, ?>) trackMatchesSchema.get("items");
        assertThat(trackItemSchema.get("required")).isEqualTo(List.of("databaseTrackId"));
        Map<?, ?> trackIdSchema = (Map<?, ?>) ((Map<?, ?>) trackItemSchema.get("properties")).get("databaseTrackId");
        assertThat(trackIdSchema.get("type")).isEqualTo(List.of("string", "null"));
        assertThat(prompts.getFirst().getInstructions().get(1).getText()).contains("spotify-a", "album-a", "2000");
        assertThat(prompts.get(2).getInstructions().get(1).getText()).contains("song-a", "song-b", "Shared title");
        assertThat(cache).hasSize(cacheEnabled ? 2 : 0);
        if (!cacheEnabled) {
            verifyNoInteractions(cacheService);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnNullWhenAllTracksAreUnmatched(boolean cacheEnabled) {
        responses.clear();
        responses.addAll(List.of("""
                {"albumMatches":[
                    {"spotifyAlbumId":"spotify-a","databaseAlbumId":null},
                    {"spotifyAlbumId":"spotify-b","databaseAlbumId":null},
                    {"spotifyAlbumId":"spotify-missing","databaseAlbumId":null}
                ]}
                """, "{\"trackMatches\":[{\"databaseTrackId\":null},{\"databaseTrackId\":null},{\"databaseTrackId\":null}]}"));

        DiscoveryTask task = task();

        assertThat(service.discover(task, cacheEnabled)).isNull();

        assertThat(task.getRawResults()).hasSize(2);
        if (cacheEnabled) {
            assertThat(service.discover(task(), true)).isNull();
        } else {
            verifyNoInteractions(cacheService);
        }
        assertThat(prompts).hasSize(4);
        assertThat(cache).hasSize(cacheEnabled ? 2 : 0);
    }

    @Test
    void shouldReuseBothCachedResponses() {
        DiscoveryTask original = task();
        service.discover(original, true);
        DiscoveryTask cached = task();

        assertThat(service.discover(cached, true)).containsExactly("song-a", "song-b", null);

        assertThat(prompts).hasSize(4);
        assertThat(cached.getRawRequests()).isEqualTo(original.getRawRequests());
        assertThat(cached.getRawResults()).isEqualTo(original.getRawResults());
    }

    @Test
    void shouldSkipFallbackWhenAllTracksMatchTheirAlbums() {
        responses.set(0, ALBUM_RESPONSE.replace("\"databaseAlbumId\":null", "\"databaseAlbumId\":\"album-a\""));
        responses.set(1, TRACK_RESPONSE.replace("\"databaseTrackId\":null", "\"databaseTrackId\":\"song-a\""));

        assertThat(service.discover(task(), true)).containsExactly("song-a", "song-b", "song-a");

        assertThat(prompts).hasSize(4);
        verifyNoInteractions(libraryService);
    }

    @ParameterizedTest
    @CsvSource({"true,true,LP", "true,false,LP", "false,true,LP", "false,false,LP",
            "true,true,COMPILATION", "true,false,COMPILATION", "false,true,COMPILATION", "false,false,COMPILATION"})
    void shouldMatchOnlyUnmatchedTracksAndCacheFallback(boolean cacheEnabled, boolean albumMatched, ReleaseType type) {
        fallbackAlbum("fallback", type, LocalDateTime.of(1990, 1, 1, 0, 0), 2020);
        if (albumMatched) {
            responses.set(0, ALBUM_RESPONSE.replace("\"databaseAlbumId\":null", "\"databaseAlbumId\":\"album-a\""));
        }
        String albumResponse = responses.getFirst();
        String fallbackResponse = "{\"trackMatches\":[{\"databaseTrackId\":\"fallback-song\"}]}";
        responses.add(fallbackResponse);
        DiscoveryTask original = task();

        assertThat(service.discover(original, cacheEnabled)).containsExactly("song-a", "song-b", "fallback-song");

        assertThat(prompts).hasSize(6);
        assertThat(prompts.get(4).getInstructions().get(1).getText())
                .contains("\"id\":\"missing\"", "fallback-song")
                .doesNotContain("\"id\":\"first\"", "\"id\":\"second\"", "\"id\":\"song-a\"", "\"id\":\"song-b\"");
        assertThat(original.getRawResults())
                .containsExactly(JsonConverter.fromJsonOrString(albumResponse), JsonConverter.fromJsonOrString(TRACK_RESPONSE), JsonConverter.fromJsonOrString(fallbackResponse));
        assertThat(cache).hasSize(cacheEnabled ? 3 : 0);
        if (cacheEnabled) {
            assertThat(cache.keySet()).anyMatch(key -> key.startsWith("SPOTIFY_TOP_TRACKS_FALLBACK:"));
            DiscoveryTask cached = task();
            assertThat(service.discover(cached, true)).containsExactly("song-a", "song-b", "fallback-song");
            assertThat(cached.getRawRequests()).isEqualTo(original.getRawRequests());
            assertThat(cached.getRawResults()).isEqualTo(original.getRawResults());
            assertThat(prompts).hasSize(6);
        } else {
            verifyNoInteractions(cacheService);
        }
    }

    private void fallbackAlbum(String id, ReleaseType type, @Nullable LocalDateTime releaseDate, @Nullable Integer year) {
        Album album = new Album().setId(id).setName(id).setYear(year).setArtist(artist);
        album.setSongs(List.of(new Song().setId(id + "-song").setName("Shared title").setAlbum(album)));
        List<Album> albums = new ArrayList<>(artist.getAlbums());
        albums.add(album);
        artist.setAlbums(albums);
        when(libraryService.getAlbumDiscoveryData(id)).thenReturn(Optional.of(new AlbumDiscoveryData(type, releaseDate)));
    }

    @Test
    void shouldTryLpsThenEpsThenCompilationsChronologicallyAndPreserveUnmatchedPositions() {
        fallbackAlbum("compilation-new", ReleaseType.COMPILATION, LocalDateTime.of(1950, 1, 1, 0, 0), 1950);
        fallbackAlbum("ep-new", ReleaseType.EP, LocalDateTime.of(1970, 1, 1, 0, 0), 1970);
        fallbackAlbum("lp-z", ReleaseType.LP, LocalDateTime.of(1990, 1, 1, 0, 0), 1900);
        fallbackAlbum("lp-undated", ReleaseType.LP, null, null);
        fallbackAlbum("ep-old", ReleaseType.EP, LocalDateTime.of(1960, 1, 1, 0, 0), 1960);
        fallbackAlbum("lp-old", ReleaseType.LP, LocalDateTime.of(1980, 1, 1, 0, 0), 2020);
        fallbackAlbum("lp-year", ReleaseType.LP, null, 1985);
        fallbackAlbum("lp-a", ReleaseType.LP, LocalDateTime.of(1990, 1, 1, 0, 0), 1900);
        fallbackAlbum("compilation-old", ReleaseType.COMPILATION, LocalDateTime.of(1940, 1, 1, 0, 0), 1940);
        List<String> expectedOrder = List.of("lp-old", "lp-year", "lp-a", "lp-z", "lp-undated", "ep-old", "ep-new",
                "compilation-old", "compilation-new");
        for (String ignored : expectedOrder) {
            responses.add("{\"trackMatches\":[{\"databaseTrackId\":null}]}");
        }

        assertThat(service.discover(task(), true)).containsExactly("song-a", "song-b", null);

        assertThat(prompts).hasSize(4 + 2 * expectedOrder.size());
        for (int i = 0; i < expectedOrder.size(); i++) {
            String request = prompts.get(4 + 2 * i).getInstructions().get(1).getText();
            assertThat(request).contains("Database album with tracks: {\"id\":\"" + expectedOrder.get(i) + "\"");
        }
    }

    @Test
    void shouldFindTracksAcrossFallbackAlbumsAndStopAfterAllAreMatched() {
        fallbackAlbum("first-lp", ReleaseType.LP, LocalDateTime.of(1990, 1, 1, 0, 0), null);
        fallbackAlbum("second-lp", ReleaseType.LP, LocalDateTime.of(2000, 1, 1, 0, 0), null);
        fallbackAlbum("unused-ep", ReleaseType.EP, LocalDateTime.of(1980, 1, 1, 0, 0), null);
        responses.set(0, ALBUM_RESPONSE.replace("\"databaseAlbumId\":\"album-a\"", "\"databaseAlbumId\":null")
                .replace("\"databaseAlbumId\":\"album-b\"", "\"databaseAlbumId\":null"));
        responses.set(1, NO_TRACK_MATCHES);
        responses.add("{\"trackMatches\":[{\"databaseTrackId\":null},{\"databaseTrackId\":\"first-lp-song\"},{\"databaseTrackId\":null}]}");
        responses.add("{\"trackMatches\":[{\"databaseTrackId\":\"second-lp-song\"},{\"databaseTrackId\":\"second-lp-song\"}]}");

        assertThat(service.discover(task(), true)).containsExactly("second-lp-song", "first-lp-song", "second-lp-song");

        assertThat(prompts).hasSize(8);
        assertThat(prompts.get(6).getInstructions().get(1).getText())
                .contains("\"id\":\"first\"", "\"id\":\"missing\"").doesNotContain("\"id\":\"second\"");
    }

    @ParameterizedTest
    @EnumSource(value = ReleaseType.class, names = {"LP", "EP", "COMPILATION"}, mode = EnumSource.Mode.EXCLUDE)
    void shouldExcludeOtherReleaseTypesFromFallback(ReleaseType type) {
        fallbackAlbum("excluded", type, LocalDateTime.of(1990, 1, 1, 0, 0), 1990);

        assertThat(service.discover(task(), true)).containsExactly("song-a", "song-b", null);

        assertThat(prompts).hasSize(4);
    }

    @Test
    void shouldReturnNullWhenNoTracksMatchAnyFallbackAlbum() {
        fallbackAlbum("fallback", ReleaseType.EP, null, 1990);
        responses.set(1, NO_TRACK_MATCHES);
        responses.add(NO_TRACK_MATCHES);

        assertThat(service.discover(task(), true)).isNull();

        assertThat(prompts).hasSize(6);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not JSON", "null", "{}", "{\"trackMatches\":[]}",
            "{\"trackMatches\":[{\"databaseTrackId\":\"song-a\"}]}",
            "{\"trackMatches\":[{\"databaseTrackId\":\"unknown\"}]}",
            "{\"trackMatches\":[{\"databaseTrackId\":123}]}",
            "{\"trackMatches\":[{}]}", "{\"trackMatches\":[null]}"})
    void shouldVerifyInvalidFallbackResponseAgainstSuppliedAlbum(String invalidResponse) {
        fallbackAlbum("fallback", ReleaseType.LP, null, 1990);
        responses.add(invalidResponse);
        responses.add("{\"trackMatches\":[{\"databaseTrackId\":\"fallback-song\"}]}");
        DiscoveryTask task = task();

        assertThat(service.discover(task, true)).containsExactly("song-a", "song-b", "fallback-song");

        assertThat(prompts).hasSize(8);
        assertThat(prompts.get(6).getInstructions()).extracting(Message::getText)
                .anySatisfy(text -> assertThat(text).contains("single supplied database album's track list"));
        assertThat(task.getRawResults()).hasSize(4);
        assertThat(cache).hasSize(3);
    }

    @Test
    void shouldFailAfterInvalidFallbackRetriesWithoutCachingResponse() {
        fallbackAlbum("fallback", ReleaseType.LP, null, 1990);
        responses.addAll(List.of("{\"trackMatches\":[]}", "{\"trackMatches\":[]}"));

        assertThatThrownBy(() -> service.discover(task(), true)).hasMessageContaining("expected 1");

        assertThat(prompts).hasSize(8);
        assertThat(cache).hasSize(2);
    }

    @Test
    void shouldInterruptDuringFallbackWithoutCallingNextAlbum() {
        fallbackAlbum("first-lp", ReleaseType.LP, null, 1990);
        fallbackAlbum("second-lp", ReleaseType.LP, null, 2000);
        responses.add("{\"trackMatches\":[{\"databaseTrackId\":null}]}");
        doAnswer(invocation -> {
            String key = invocation.getArgument(1);
            cache.put(key, invocation.getArgument(3));
            if (key.startsWith("SPOTIFY_TOP_TRACKS_FALLBACK:")) {
                jobSynchronizer.cancelDiscovery();
            }
            return null;
        }).when(cacheService).put(eq(SPOTIFY), anyString(), eq(1), anyString());

        assertThatThrownBy(() -> service.discover(task(), true)).isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(prompts).hasSize(6);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Here is the result:\n%s", "%s\nDone.", "```json\n%s\n```",
            "Here is the result:\n```json\n%s\n```\nDone."})
    void shouldExtractAlbumAndTrackJsonWithoutRequestingVerification(String responseTemplate) {
        String albumResponse = responseTemplate.formatted(ALBUM_RESPONSE);
        String trackResponse = responseTemplate.formatted(TRACK_RESPONSE);
        responses.set(0, albumResponse);
        responses.set(1, trackResponse);
        DiscoveryTask task = task();

        assertThat(service.discover(task, true)).containsExactly("song-a", "song-b", null);
        assertThat(service.discover(task(), true)).containsExactly("song-a", "song-b", null);

        assertThat(prompts).hasSize(4);
        assertThat(task.getRawResults()).containsExactly(JsonConverter.fromJsonOrString(albumResponse), JsonConverter.fromJsonOrString(trackResponse));
        assertThat(cache).hasSize(2);
        verifyNoInteractions(logService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not JSON", "null", "[]", "{}", "{\"spotify-a\":\"album-a\"}",
            "{\"albumMatches\":null}", "{\"albumMatches\":{}}", "{\"albumMatches\":[null]}",
            "{\"albumMatches\":[{\"spotifyAlbumId\":\"spotify-a\"}]}",
            "{\"albumMatches\":[{\"spotifyAlbumId\":\"spotify-a\",\"databaseAlbumId\":\"album-a\"}]}",
            "{\"albumMatches\":[{\"spotifyAlbumId\":\"unknown\",\"databaseAlbumId\":\"album-a\"}]}",
            "{\"albumMatches\":[{\"spotifyAlbumId\":\"spotify-a\",\"databaseAlbumId\":\"foreign-album\"}]}",
            "{\"albumMatches\":[{\"spotifyAlbumId\":\"spotify-a\",\"databaseAlbumId\":123}]}",
            "{\"albumMatches\":[{\"spotifyAlbumId\":\"spotify-a\",\"databaseAlbumId\":null},{\"spotifyAlbumId\":\"spotify-a\",\"databaseAlbumId\":null}]}",
            "Result: {albumMatches: []}", "Result: {\"albumMatches\":[]}"})
    void shouldVerifyInvalidAlbumResponse(String invalidResponse) {
        responses.addFirst(invalidResponse);
        DiscoveryTask task = task();

        assertThat(service.discover(task, true)).containsExactly("song-a", "song-b", null);

        assertThat(prompts).hasSize(6);
        assertThat(prompts.get(2).getInstructions()).extracting(Message::getText)
                .anySatisfy(text -> assertThat(text).contains("Recheck the entire answer"));
        assertThat(task.getRawResults()).containsExactly(JsonConverter.fromJsonOrString(invalidResponse), JsonConverter.fromJsonOrString(ALBUM_RESPONSE), JsonConverter.fromJsonOrString(TRACK_RESPONSE));
        assertThat(cache).hasSize(2);
        verify(logService).warn(any(), eq("Invalid Spotify top tracks response from the LLM for artist '{}'."),
                eq(artist.getName()), any(RuntimeException.class));
    }

    @Test
    void shouldFailAfterSecondIncompleteAlbumResponseWithoutCachingIt() {
        responses.clear();
        String incompleteResponse = "{\"albumMatches\":[]}";
        responses.addAll(List.of(incompleteResponse, incompleteResponse));
        DiscoveryTask task = task();

        assertThatThrownBy(() -> service.discover(task, true)).hasMessageContaining("must include every supplied Spotify album ID");

        assertThat(prompts).hasSize(4);
        assertThat(task.getRawResults()).containsExactly(JsonConverter.fromJsonOrString(incompleteResponse), JsonConverter.fromJsonOrString(incompleteResponse));
        assertThat(cache).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not JSON", "null", "{}", "[]", "[\"song-a\",\"song-b\",null]",
            "{\"trackMatches\":null}", "{\"trackMatches\":{}}", "{\"trackMatches\":[]}",
            "{\"trackMatches\":[{\"databaseTrackId\":\"song-a\"},{\"databaseTrackId\":null}]}",
            "{\"trackMatches\":[{\"databaseTrackId\":\"unknown\"},{\"databaseTrackId\":\"song-b\"},{\"databaseTrackId\":null}]}",
            "{\"trackMatches\":[{\"databaseTrackId\":\"song-b\"},{\"databaseTrackId\":\"song-a\"},{\"databaseTrackId\":null}]}",
            "{\"trackMatches\":[{\"databaseTrackId\":\"song-a\"},{\"databaseTrackId\":\"song-b\"},{\"databaseTrackId\":\"song-a\"}]}",
            "{\"trackMatches\":[{\"databaseTrackId\":\"song-a\"},{\"databaseTrackId\":123},{\"databaseTrackId\":null}]}",
            "{\"trackMatches\":[{\"databaseTrackId\":\"song-a\"},{\"databaseTrackId\":\"song-b\"},{}]}",
            "{\"trackMatches\":[{\"databaseTrackId\":\"song-a\"},{\"databaseTrackId\":\"song-b\"},null]}",
            "Result: {trackMatches: []}",
            "Result: {\"trackMatches\":[{\"databaseTrackId\":\"unknown\"},{\"databaseTrackId\":\"song-b\"},{\"databaseTrackId\":null}]}"})
    void shouldVerifyInvalidTrackResponse(String invalidResponse) {
        responses.add(1, invalidResponse);
        DiscoveryTask task = task();

        assertThat(service.discover(task, true)).containsExactly("song-a", "song-b", null);

        assertThat(prompts).hasSize(6);
        assertThat(prompts.get(prompts.size() - 2).getInstructions()).extracting(Message::getText)
                .anySatisfy(text -> assertThat(text).contains("Recheck the entire answer"));
        assertThat(task.getRawResults()).containsExactly(JsonConverter.fromJsonOrString(ALBUM_RESPONSE), JsonConverter.fromJsonOrString(invalidResponse), JsonConverter.fromJsonOrString(TRACK_RESPONSE));
        assertThat(cache).hasSize(2);
    }

    @Test
    void shouldFailAfterSecondInvalidTrackResponseWithoutCachingIt() {
        responses.set(1, "{\"trackMatches\":[]}");
        responses.add("{\"trackMatches\":[]}");

        assertThatThrownBy(() -> service.discover(task(), true)).hasMessageContaining("expected 3");

        assertThat(prompts).hasSize(6);
        assertThat(cache).hasSize(1);
    }

    @Test
    void shouldReturnNoTracksWhenCompletedArtistDataIsEmpty() {
        previousTask.setResult(null);

        assertThat(service.discover(task(), true)).isEmpty();

        verifyNoInteractions(model, cacheService, artistRepository);
    }

    @Test
    void shouldFailBeforeCallingLlmWhenNoCompletedArtistDataTaskExists() {
        when(taskRepository.findLatestCompletedByArtistIdAndType("artist", DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.discover(task(), true)).hasMessageContaining("No completed SPOTIFY_ARTIST_DATA task").hasMessageContaining("artist");

        verify(model, never()).call(any(Prompt.class));
        verifyNoInteractions(cacheService, artistRepository);
    }

    @Test
    void shouldStopBeforeMatchingWhenCancelled() {
        jobSynchronizer.cancelDiscovery();

        assertThatThrownBy(() -> service.discover(task(), true)).isInstanceOf(DiscoveryInterruptedException.class);

        verify(model, never()).call(any(Prompt.class));
        verifyNoInteractions(cacheService, taskRepository, artistRepository);
    }

    private DiscoveryTask task() {
        return new DiscoveryTask().setType(DiscoveryTaskType.SPOTIFY_TOP_TRACKS)
                .setArtist(artist);
    }

    private SpotifyArtistData.TopTrack topTrack(String id, String albumId) {
        return new SpotifyArtistData.TopTrack(id, "Shared title", "track-url", "Album", albumId, "album-url");
    }
}
