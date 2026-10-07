package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.api.llm.service.LlmCacheService;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.ShutdownService;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.dorokhov.pony2.api.llm.domain.LlmCacheRegion.SPOTIFY;
import static net.dorokhov.pony2.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotifyArtistDataServiceTest {

    private static final String ARTIST_ID = "a".repeat(22);
    private static final String ALBUM_ID = "b".repeat(22);

    @Mock private ChatModel model;
    @Mock private LlmCacheService cacheService;
    @Mock private ArtistRepository artistRepository;
    @Mock private LogService logService;

    private SpotifyArtistDataService service;
    private ChatClient configuredClient;
    private ValidatorFactory validatorFactory;
    private Validator validator;
    private final ShutdownService shutdownService = new ShutdownService();
    private final Map<String, String> cache = new HashMap<>();
    private final List<Prompt> prompts = new ArrayList<>();
    private int modelCalls;
    private int artistCount;
    private String response;

    @BeforeEach
    void setUp() throws Exception {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
        lenient().when(cacheService.get(eq(SPOTIFY), anyString(), eq(1)))
                .thenAnswer(invocation -> Optional.ofNullable(cache.get(invocation.getArgument(1))));
        lenient().when(cacheService.put(eq(SPOTIFY), anyString(), eq(1), anyString())).thenAnswer(invocation -> {
            cache.put(invocation.getArgument(1), invocation.getArgument(3));
            return null;
        });
        lenient().when(model.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        lenient().when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            prompts.add(prompt);
            modelCalls++;
            return new ChatResponse(List.of(new Generation(new AssistantMessage(response))));
        });
        response = JsonConverter.toJson(found());
        configuredClient = ChatClient.builder(model).build();
        service = createService(configuredClient, new ClassPathResource("prompts/spotify-artist-data.txt"));
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void shouldReuseConfiguredClientAndCacheResponse() throws IOException {
        configuredClient = ChatClient.builder(model).defaultTools(tool("browser_navigate"), tool("fetch_url")).build();
        service = createService(configuredClient, new ClassPathResource("prompts/spotify-artist-data.txt"));
        Artist artist = artist();

        assertThat(service.discover(artist.getId())).contains(found());
        assertThat(service.discover(artist.getId())).contains(found());

        assertThat(modelCalls).isOne();
        assertThat(prompts.getFirst().getInstructions()).anySatisfy(message ->
                assertThat(message.getText()).contains("Playwright MCP", "browser_navigate", "open.spotify.com"));
        assertThat(((ToolCallingChatOptions) prompts.getFirst().getOptions()).getToolCallbacks())
                .extracting(tool -> tool.getToolDefinition().name()).containsExactly("browser_navigate", "fetch_url");
        assertThat(cache).hasSize(1);
        Map<?, ?> entry = JsonConverter.fromJson(cache.values().iterator().next(), Map.class);
        Map<?, ?> request = (Map<?, ?>) entry.get("request");
        assertThat(request.get("systemPrompt")).isEqualTo(prompts.getFirst().getInstructions().getFirst().getText());
        assertThat(request.get("userPrompt")).isEqualTo(prompts.getFirst().getInstructions().get(1).getText());
        assertThat(request.get("albumTitles")).isEqualTo(List.of("Album"));
        assertThat(entry.get("response")).isEqualTo(response);
    }

    @Test
    void shouldInvalidateCacheWhenAlbumsOrPromptChanges() throws Exception {
        Artist original = artist();
        service.discover(original.getId());
        service.discover(artist("Artist", new Album().setName("Album"), new Album().setName("Second")).getId());
        String changedPrompt = new ClassPathResource("prompts/spotify-artist-data.txt").getContentAsString(UTF_8)
                + "\nExtra instruction";
        SpotifyArtistDataService changedService = createService(configuredClient, new ByteArrayResource(changedPrompt.getBytes(UTF_8)));
        changedService.discover(original.getId());

        assertThat(cache).hasSize(3);
        assertThat(modelCalls).isEqualTo(3);
    }

    @Test
    void shouldPropagateConfiguredClientFailureWithoutCaching() {
        when(model.call(any(Prompt.class))).thenThrow(new IllegalStateException("Playwright is unavailable"));

        assertThatThrownBy(() -> service.discover(artist().getId())).hasMessageContaining("Playwright is unavailable");
        assertThat(cache).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "null", "not JSON"})
    void shouldNotCacheInvalidJson(String invalidResponse) {
        response = invalidResponse;
        assertThatThrownBy(() -> service.discover(artist().getId())).isInstanceOf(RuntimeException.class);
        assertThat(cache).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = SpotifyArtistData.Status.class, names = {"NOT_FOUND", "AMBIGUOUS"})
    void shouldCacheUnconfirmedArtists(SpotifyArtistData.Status status) {
        Artist artist = artist();
        SpotifyArtistData result = new SpotifyArtistData(status, null, null, null, null, null, null, null);
        response = JsonConverter.toJson(result);

        assertThat(service.discover(artist.getId())).contains(result);
        assertThat(service.discover(artist.getId())).contains(result);

        assertThat(cache).hasSize(1);
        assertThat(modelCalls).isOne();
    }

    @Test
    void shouldRejectAlbumThatWasNotSupplied() {
        SpotifyArtistData data = found();
        response = JsonConverter.toJson(new SpotifyArtistData(data.status(), data.artist(),
                new SpotifyArtistData.MatchedAlbum("Other album", "Album", ALBUM_ID, albumUrl()),
                data.topTracks(), data.similarArtists(), data.biography(), data.links(), data.imageUrl()));
        assertThatThrownBy(() -> service.discover(artist().getId())).hasMessageContaining("invalid Spotify response");
        assertThat(cache).isEmpty();
    }

    @Test
    void shouldAcceptAndCacheDifferentSpotifyUrlAndIdFormats() {
        String albumUrl = "https://open.spotify.com/intl-de/release/album-id-v2?si=example";
        SpotifyArtistData data = new SpotifyArtistData(SpotifyArtistData.Status.FOUND,
                new SpotifyArtistData.SpotifyArtist("artist-id-v2", "Artist",
                        "https://artists.spotify.com/profile/artist-id-v2?locale=de"),
                new SpotifyArtistData.MatchedAlbum("Album", "Album", "album-id-v2", albumUrl),
                List.of(new SpotifyArtistData.TopTrack("track-id-v2", "Track",
                        "https://open.spotify.com/intl-de/song/track-id-v2?si=example#details",
                        "Album", "album-id-v2", albumUrl)),
                List.of(new SpotifyArtistData.SpotifyArtist("related-id-v2", "Related artist",
                        "https://open.spotify.com/intl-de/artist/related-id-v2?si=example")),
                null, null, null);
        response = JsonConverter.toJson(data);
        Artist artist = artist();

        assertThat(service.discover(artist.getId())).contains(data);
        assertThat(service.discover(artist.getId())).contains(data);
        assertThat(modelCalls).isOne();
    }

    @Test
    void shouldAllowUnavailableOptionalSections() {
        SpotifyArtistData data = found();
        SpotifyArtistData partial = new SpotifyArtistData(data.status(), data.artist(), data.matchedAlbum(),
                null, null, null, null, null);
        response = JsonConverter.toJson(partial);

        assertThat(service.discover(artist().getId())).contains(partial);
    }

    @ParameterizedTest
    @MethodSource("invalidResponses")
    void shouldRejectInvalidFieldsOrConfirmedArtistWithoutIdentification(SpotifyArtistData invalidResponse) {
        response = JsonConverter.toJson(invalidResponse);
        assertThatThrownBy(() -> service.discover(artist().getId())).hasMessageContaining("invalid Spotify response");
        assertThat(cache).isEmpty();
    }

    @Test
    void shouldNotCallLlmOnShutdown() {
        shutdownService.onApplicationEvent();

        assertThatThrownBy(() -> service.discover(artist().getId())).isInstanceOf(DiscoveryInterruptedException.class);
        assertThat(cache).isEmpty();
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void shouldCacheSuccessfulLlmResponseDuringShutdown() {
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            shutdownService.onApplicationEvent();
            return new ChatResponse(List.of(new Generation(new AssistantMessage(response))));
        });

        assertThat(service.discover(artist().getId())).contains(found());
        assertThat(cache).hasSize(1);
    }

    @Test
    void shouldPreserveAlbumListAndEscapeNamesAsData() {
        Artist artist = artist("Artist {name}",
                new Album().setName(null), new Album().setName("Album\n\"Quoted\"").setYear(2000));
        response = JsonConverter.toJson(new SpotifyArtistData(SpotifyArtistData.Status.NOT_FOUND,
                null, null, null, null, null, null, null));

        service.discover(artist.getId());

        assertThat(prompts.getFirst().getInstructions().get(1).getText())
                .contains("\"Artist {name}\"", "- null", "- \"Album\\n\\\"Quoted\\\"\" (year: 2000)");
    }

    @Test
    void shouldSortAlbumsUsingTheirNaturalOrderAndReuseCacheRegardlessOfInputOrder() {
        Album earlier = new Album().setName("Z album").setYear(2000);
        Album later = new Album().setName("Album").setYear(2010);
        Artist artist = artist("Artist", later, earlier);

        service.discover(artist.getId());
        artist.setAlbums(List.of(earlier, later));
        service.discover(artist.getId());

        assertThat(prompts.getFirst().getInstructions().get(1).getText())
                .contains("- \"Z album\" (year: 2000)\n- \"Album\" (year: 2010)");
        assertThat(cache).hasSize(1);
        assertThat(modelCalls).isOne();
    }

    @Test
    void shouldSkipArtistWithoutAlbums() {
        Artist artist = artist("Artist");

        assertThat(service.discover(artist.getId())).isEmpty();

        verify(logService).info(any(), contains("no album title"), eq("Artist"), eq(artist.getId()));
        verify(model, never()).call(any(Prompt.class));
        verifyNoInteractions(cacheService);
    }

    @Test
    void shouldSkipArtistWithoutName() {
        Artist artist = artist(null, new Album().setName("Album"));

        assertThat(service.discover(artist.getId())).isEmpty();

        verify(logService).info(any(), contains("artist name is unknown"), eq(artist.getId()));
        verify(model, never()).call(any(Prompt.class));
        verifyNoInteractions(cacheService);
    }

    private SpotifyArtistDataService createService(ChatClient client, Resource promptResource) throws IOException {
        return new SpotifyArtistDataService(client,
                cacheService, validator, artistRepository, logService, shutdownService, transactionManager(), promptResource);
    }

    private Artist artist() {
        return artist("Artist", new Album().setName("Album"));
    }

    private Artist artist(String name, Album... albums) {
        Artist artist = new Artist().setId("artist-" + artistCount++).setName(name).setAlbums(List.of(albums));
        artist.getAlbums().forEach(album -> album.setArtist(artist));
        lenient().when(artistRepository.findById(artist.getId())).thenReturn(Optional.of(artist));
        return artist;
    }

    private static SpotifyArtistData found() {
        return new SpotifyArtistData(SpotifyArtistData.Status.FOUND,
                new SpotifyArtistData.SpotifyArtist(ARTIST_ID, "Artist", "https://open.spotify.com/artist/" + ARTIST_ID),
                new SpotifyArtistData.MatchedAlbum("Album", "Album", ALBUM_ID, albumUrl()),
                List.of(new SpotifyArtistData.TopTrack("c".repeat(22), "Top track",
                        "https://open.spotify.com/track/" + "c".repeat(22), "Album", ALBUM_ID, albumUrl())),
                List.of(new SpotifyArtistData.SpotifyArtist("d".repeat(22), "Related artist",
                        "https://open.spotify.com/artist/" + "d".repeat(22))),
                "Artist biography", List.of(new SpotifyArtistData.ExternalLink("Website", "https://example.com/")),
                "https://i.scdn.co/image/artist");
    }

    private static String albumUrl() {
        return "https://open.spotify.com/album/" + ALBUM_ID;
    }

    private static Stream<SpotifyArtistData> invalidResponses() {
        SpotifyArtistData data = found();
        return Stream.of(
                new SpotifyArtistData(null, null, null, null, null, null, null, null),
                new SpotifyArtistData(data.status(), null, data.matchedAlbum(), null, null, null, null, null),
                new SpotifyArtistData(data.status(), data.artist(), null, null, null, null, null, null),
                new SpotifyArtistData(data.status(), new SpotifyArtistData.SpotifyArtist("", "Artist", "url"),
                        data.matchedAlbum(), null, null, null, null, null));
    }

    private ToolCallback tool(String name) {
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name(name).description(name).inputSchema("{\"type\":\"object\"}").build());
        return callback;
    }
}
