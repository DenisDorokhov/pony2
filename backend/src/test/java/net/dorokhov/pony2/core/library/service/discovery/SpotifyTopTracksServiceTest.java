package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.Song;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.api.llm.service.LlmCacheService;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.core.io.ClassPathResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.dorokhov.pony2.api.llm.domain.LlmCacheRegion.SPOTIFY;
import static net.dorokhov.pony2.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotifyTopTracksServiceTest {

    private static final String ALBUM_RESPONSE = "{\"spotify-a\":\"album-a\",\"spotify-b\":\"album-b\",\"spotify-missing\":null}";
    private static final String TRACK_RESPONSE = "[\"song-a\",\"song-b\",null]";

    @Mock private ChatModel model;
    @Mock private LlmCacheService cacheService;
    @Mock private ArtistRepository artistRepository;
    @Mock private DiscoveryTaskRepository taskRepository;
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
        SpotifyArtistData data = new SpotifyArtistData(SpotifyArtistData.Status.FOUND,
                new SpotifyArtistData.SpotifyArtist("spotify-artist", "Artist", "artist-url"),
                new SpotifyArtistData.MatchedAlbum("First album", "First album", "spotify-a", "album-url"),
                List.of(topTrack("first", "spotify-a"), topTrack("second", "spotify-b"), topTrack("missing", "spotify-missing")),
                null, null, null, null);
        previousTask = new DiscoveryTask().setId("previous").setStatus(DiscoveryTask.Status.COMPLETE).setResult(JsonConverter.toJson(data));
        lenient().when(taskRepository.findById("previous")).thenReturn(Optional.of(previousTask));
        lenient().when(artistRepository.findById("artist")).thenReturn(Optional.of(artist));
        lenient().when(model.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        lenient().when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            prompts.add(invocation.getArgument(0));
            return new ChatResponse(List.of(new Generation(new AssistantMessage(responses.removeFirst()))));
        });
        lenient().when(cacheService.get(eq(SPOTIFY), anyString(), eq(1)))
                .thenAnswer(invocation -> Optional.ofNullable(cache.get(invocation.getArgument(1))));
        lenient().when(cacheService.put(eq(SPOTIFY), anyString(), eq(1), anyString())).thenAnswer(invocation -> {
            cache.put(invocation.getArgument(1), invocation.getArgument(3));
            return null;
        });
        ChatClient client = ChatClient.builder(model).build();
        DiscoveryChatClient discoveryClient = new DiscoveryChatClient(customizer -> client,
                jobSynchronizer, new DiscoveryAdvisor(shutdownService, jobSynchronizer));
        DiscoveryTaskLlmExecutor executor = new DiscoveryTaskLlmExecutor(discoveryClient, cacheService,
                taskRepository, jobSynchronizer, shutdownService, transactionManager());
        service = new SpotifyTopTracksService(executor, artistRepository, taskRepository, logService,
                shutdownService, jobSynchronizer, transactionManager(),
                new ClassPathResource("prompts/spotify-top-tracks-albums.txt"),
                new ClassPathResource("prompts/spotify-top-tracks.txt"),
                new ClassPathResource("prompts/spotify-top-tracks-verification.txt"));
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

        assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(ALBUM_RESPONSE, TRACK_RESPONSE);
        assertThat(prompts).hasSize(2);
        assertThat(prompts.getFirst().getInstructions().get(1).getText()).contains("spotify-a", "album-a", "2000");
        assertThat(prompts.getLast().getInstructions().get(1).getText()).contains("song-a", "song-b", "Shared title");
        assertThat(cache).hasSize(cacheEnabled ? 2 : 0);
        if (!cacheEnabled) {
            verifyNoInteractions(cacheService);
        }
    }

    @Test
    void shouldAcceptAllUnmatchedTracks() {
        responses.clear();
        responses.addAll(List.of("{\"spotify-a\":null,\"spotify-b\":null,\"spotify-missing\":null}", "[null,null,null]"));

        assertThat(service.discover(task(), true)).containsExactly(null, null, null);

        assertThat(prompts).hasSize(2);
        assertThat(cache).hasSize(2);
    }

    @Test
    void shouldReuseBothCachedResponses() {
        DiscoveryTask original = task();
        service.discover(original, true);
        DiscoveryTask cached = task();

        assertThat(service.discover(cached, true)).containsExactly("song-a", "song-b", null);

        assertThat(prompts).hasSize(2);
        assertThat(cached.getRawRequest()).isEqualTo(original.getRawRequest());
        assertThat(cached.getRawResult()).isEqualTo(original.getRawResult());
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

        assertThat(prompts).hasSize(2);
        assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(albumResponse, trackResponse);
        assertThat(cache).hasSize(2);
        verifyNoInteractions(logService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not JSON", "null", "[]", "{}", "{\"spotify-a\":\"album-a\"}",
            "{\"spotify-a\":\"album-a\",\"spotify-b\":\"album-b\"}", "{\"unknown\":\"album-a\"}",
            "{\"spotify-a\":\"foreign-album\"}", "{\"spotify-a\":123}", "Result: {spotify_a: 'album-a'}",
            "Result: {\"spotify-a\":\"album-a\"}"})
    void shouldVerifyInvalidAlbumResponse(String invalidResponse) {
        responses.addFirst(invalidResponse);
        DiscoveryTask task = task();

        assertThat(service.discover(task, true)).containsExactly("song-a", "song-b", null);

        assertThat(prompts).hasSize(3);
        assertThat(prompts.get(1).getInstructions().getLast().getText()).contains("Recheck the entire answer");
        assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(invalidResponse, ALBUM_RESPONSE, TRACK_RESPONSE);
        assertThat(cache).hasSize(2);
    }

    @Test
    void shouldFailAfterSecondIncompleteAlbumResponseWithoutCachingIt() {
        responses.clear();
        responses.addAll(List.of("{}", "{}"));
        DiscoveryTask task = task();

        assertThatThrownBy(() -> service.discover(task, true)).hasMessageContaining("must include every supplied Spotify album ID");

        assertThat(prompts).hasSize(2);
        assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly("{}", "{}");
        assertThat(cache).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not JSON", "null", "{}", "[]", "[\"song-a\",null]",
            "[\"unknown\",\"song-b\",null]", "[\"song-b\",\"song-a\",null]",
            "[\"song-a\",\"song-b\",\"song-a\"]", "[\"song-a\",123,null]",
            "Result: ['song-a','song-b',null]", "Result: [\"unknown\",\"song-b\",null]"})
    void shouldVerifyInvalidTrackResponse(String invalidResponse) {
        responses.add(1, invalidResponse);
        DiscoveryTask task = task();

        assertThat(service.discover(task, true)).containsExactly("song-a", "song-b", null);

        assertThat(prompts).hasSize(3);
        assertThat(prompts.getLast().getInstructions().getLast().getText()).contains("Recheck the entire answer");
        assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(ALBUM_RESPONSE, invalidResponse, TRACK_RESPONSE);
        assertThat(cache).hasSize(2);
    }

    @Test
    void shouldFailAfterSecondInvalidTrackResponseWithoutCachingIt() {
        responses.set(1, "[]");
        responses.add("[]");

        assertThatThrownBy(() -> service.discover(task(), true)).hasMessageContaining("expected 3");

        assertThat(prompts).hasSize(3);
        assertThat(cache).hasSize(1);
    }

    @Test
    void shouldFailBeforeCallingLlmWhenPreviousTaskFailed() {
        previousTask.setStatus(DiscoveryTask.Status.FAILED).setResult("{\"error\":\"failure\"}");

        assertThatThrownBy(() -> service.discover(task(), true)).hasMessageContaining("SPOTIFY_ARTIST_DATA").hasMessageContaining("FAILED");

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
        return new DiscoveryTask().setParameter(JsonConverter.toJson(new DiscoveryTask.SpotifyTopTracksParameter("artist", "previous")));
    }

    private SpotifyArtistData.TopTrack topTrack(String id, String albumId) {
        return new SpotifyArtistData.TopTrack(id, "Shared title", "track-url", "Album", albumId, "album-url");
    }
}
