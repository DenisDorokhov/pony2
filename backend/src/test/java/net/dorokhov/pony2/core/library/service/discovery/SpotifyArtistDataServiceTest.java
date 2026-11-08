package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.api.library.service.DiscoveryCacheService;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.common.LlmJsonConverter;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.ShutdownService;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.discovery.spotify.SpotifyArtistDataService;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.dorokhov.pony2.test.SpotifyArtistDataFixtures.spotifyArtistResponse;
import static net.dorokhov.pony2.api.library.domain.DiscoveryCacheRegion.SPOTIFY;
import static net.dorokhov.pony2.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotifyArtistDataServiceTest {

    private static final String ARTIST_ID = "a".repeat(22);
    private static final String ALBUM_ID = "b".repeat(22);

    @Mock private ChatModel model;
    @Mock private DiscoveryCacheService cacheService;
    @Mock private ArtistRepository artistRepository;
    @Mock private DiscoveryTaskRepository discoveryTaskRepository;
    @Mock private LogService logService;

    private SpotifyArtistDataService service;
    private ChatClient configuredClient;
    private ValidatorFactory validatorFactory;
    private Validator validator;
    private final ShutdownService shutdownService = new ShutdownService();
    private final LibraryJobSynchronizer jobSynchronizer = new LibraryJobSynchronizer(shutdownService);
    private LibraryJobSynchronizer.LibraryJobRegistration jobRegistration;
    private final Map<String, String> cache = new HashMap<>();
    private final List<Prompt> prompts = new ArrayList<>();
    private final List<String> queuedResponses = new ArrayList<>();
    private final List<String> collectedResponses = new ArrayList<>();
    private final List<String> queuedFormattingResponses = new ArrayList<>();
    private int modelCalls;
    private int artistCount;
    private String response;

    @BeforeEach
    void setUp() throws Exception {
        jobRegistration = jobSynchronizer.registerDiscoveryJob();
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
        lenient().when(cacheService.get(eq(SPOTIFY), anyString(), eq(1)))
                .thenAnswer(invocation -> Optional.ofNullable(cache.get(invocation.getArgument(1, String.class))));
        lenient().when(cacheService.put(eq(SPOTIFY), anyString(), eq(1), anyString())).thenAnswer(invocation -> {
            cache.put(invocation.getArgument(1), invocation.getArgument(3));
            return null;
        });
        lenient().when(model.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
        lenient().when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            prompts.add(prompt);
            modelCalls++;
            if (requireNonNull(requireNonNull(((OpenAiChatOptions) prompt.getOptions())).getResponseFormat()).getType() == ResponseFormat.Type.TEXT) {
                String nextResponse = queuedResponses.isEmpty() ? response : queuedResponses.removeFirst();
                String stageResponse = stageResponse(prompt, nextResponse);
                collectedResponses.add(stageResponse);
                return new ChatResponse(List.of(new Generation(new AssistantMessage(stageResponse))));
            }
            String nextResponse = queuedFormattingResponses.isEmpty()
                    ? prompt.getInstructions().getLast().getText() : queuedFormattingResponses.removeFirst();
            String stageResponse = stageResponse(prompt, nextResponse);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(stageResponse))));
        });
        response = spotifyArtistResponse(found());
        configuredClient = ChatClient.builder(model).build();
        service = createService(configuredClient, new ClassPathResource("prompts/spotify-artist-identity.txt"));
    }

    @AfterEach
    void tearDown() {
        jobRegistration.close();
        validatorFactory.close();
    }

    @Test
    void shouldReuseConfiguredClientAndCacheResponse() throws IOException {
        configuredClient = ChatClient.builder(model).defaultTools(tool("browser_navigate"), tool("fetch_url")).build();
        service = createService(configuredClient, new ClassPathResource("prompts/spotify-artist-identity.txt"));
        Artist artist = artist();

        assertThat(service.discover(task(artist), true)).isEqualTo(found());
        assertThat(service.discover(task(artist), true)).isEqualTo(found());

        assertThat(modelCalls).isEqualTo(5);
        assertThat(prompts).allSatisfy(prompt -> assertThat(
                requireNonNull(requireNonNull(((OpenAiChatOptions) prompt.getOptions())).getResponseFormat()).getType())
                .isEqualTo(ResponseFormat.Type.TEXT));
        assertThat(prompts.getFirst().getInstructions()).anySatisfy(message ->
                assertThat(message.getText()).contains("Playwright MCP", "browser_navigate", "open.spotify.com"));
        assertThat(requireNonNull(((ToolCallingChatOptions) prompts.getFirst().getOptions())).getToolCallbacks())
                .extracting(tool -> tool.getToolDefinition().name()).containsExactly("browser_navigate", "fetch_url");
        assertThat(cache).hasSize(5);
        Map<?, ?> entry = cacheEntry("SPOTIFY_ARTIST_IDENTITY");
        Map<?, ?> request = (Map<?, ?>) entry.get("request");
        assertThat(request).hasSize(4);
        assertThat(request.get("systemPrompt")).isEqualTo(prompts.getFirst().getInstructions().getFirst().getText());
        assertThat(request.get("userPrompt")).isEqualTo(prompts.getFirst().getInstructions().get(1).getText());
        assertThat(request.get("albumTitles")).isEqualTo(List.of("Album"));
        assertThat(entry.get("response")).isEqualTo(collectedResponses.getFirst());
        Map<?, ?> schema = collectionSchema(prompts.getFirst());
        assertThat(requireNonNull(((OpenAiChatOptions) prompts.getFirst().getOptions()).getResponseFormat()).getType())
                .isEqualTo(ResponseFormat.Type.TEXT);
        assertThat(request.get("responseSchema")).isEqualTo(schema);
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
        assertThat((List<?>) schema.get("required")).extracting(Object::toString).containsExactlyInAnyOrder(
                "status", "artist", "matchedAlbum", "avatarUrl");
        Map<?, ?> properties = (Map<?, ?>) schema.get("properties");
        Map<?, ?> avatarSchema = (Map<?, ?>) properties.get("avatarUrl");
        assertThat((List<?>) avatarSchema.get("type")).extracting(Object::toString).containsExactlyInAnyOrder("string", "null");
        for (String field : List.of("artist", "matchedAlbum", "avatarUrl")) {
            Map<?, ?> property = (Map<?, ?>) properties.get(field);
            if (property.get("type") instanceof List<?> types) {
                assertThat(types).extracting(Object::toString).contains("null");
            } else {
                assertThat((List<?>) property.get("anyOf")).anySatisfy(option ->
                        assertThat(((Map<?, ?>) option).get("type")).isEqualTo("null"));
            }
        }
        assertThat(prompts.getFirst().getInstructions().getFirst().getText()).doesNotContain("```", "\"$schema\"");
    }

    private Map<?, ?> collectionSchema(Prompt prompt) {
        String schemaInstruction = prompt.getInstructions().getLast().getText();
        return JsonConverter.fromJson(schemaInstruction.substring(schemaInstruction.indexOf("\n\n") + 2), Map.class);
    }

    @Test
    void shouldSkipCacheReadsAndWritesWhenDisabled() {
        Artist artist = artist();

        assertThat(service.discover(task(artist), true)).isEqualTo(found());
        Map<String, String> originalCache = Map.copyOf(cache);
        clearInvocations(cacheService);
        response = spotifyArtistResponse("NOT_FOUND");

        assertThat(service.discover(task(artist), false)).isNull();
        assertThat(service.discover(task(artist), false)).isNull();

        verifyNoInteractions(cacheService);
        assertThat(modelCalls).isEqualTo(7);
        assertThat(cache).isEqualTo(originalCache);

        assertThat(service.discover(task(artist), true)).isEqualTo(found());
        assertThat(modelCalls).isEqualTo(7);
    }

    @Test
    void shouldNotPopulateCacheWhenDisabled() {
        Artist artist = artist();

        assertThat(service.discover(task(artist), false)).isEqualTo(found());

        verifyNoInteractions(cacheService);
        assertThat(cache).isEmpty();
        assertThat(modelCalls).isEqualTo(5);
    }

    @Test
    void shouldInvalidateCacheWhenAlbumsOrPromptChanges() throws Exception {
        Artist original = artist();
        service.discover(task(original), true);
        service.discover(task(artist("Artist", new Album().setName("Album"), new Album().setName("Second"))), true);
        String changedPrompt = new ClassPathResource("prompts/spotify-artist-identity.txt").getContentAsString(UTF_8)
                + "\nExtra instruction";
        SpotifyArtistDataService changedService = createService(configuredClient, new ByteArrayResource(changedPrompt.getBytes(UTF_8)));
        changedService.discover(task(original), true);

        assertThat(cache).hasSize(7);
        assertThat(modelCalls).isEqualTo(7);
    }

    @Test
    void shouldStartEachStageWithOnlyItsOwnInstructionsAndConfirmedArtist() {
        Artist artist = artist("Artist", new Album().setName("Album"), new Album().setName("Private library album"));

        assertThat(service.discover(task(artist), false)).isEqualTo(found());

        assertThat(prompts).hasSize(5);
        for (int i = 1; i < prompts.size(); i++) {
            Prompt collection = prompts.get(i);
            assertThat(collection.getInstructions()).hasSize(3).noneMatch(AssistantMessage.class::isInstance);
            assertThat(collection.getInstructions().get(1).getText())
                    .isEqualTo("Confirmed Spotify artist: " + JsonConverter.toJson(found().artist()))
                    .doesNotContain("Private library album", found().biography(), "Top track", "Related artist");
        }
    }

    @Test
    void shouldKeepFollowUpHistoryInsideTheFailedStage() {
        queuedResponses.add(response);
        queuedResponses.add(response);
        queuedResponses.add("Invalid biography response");
        DiscoveryTask task = task(artist());

        assertThat(service.discover(task, true)).isEqualTo(found());

        assertThat(modelCalls).isEqualTo(7);
        Prompt originalBiography = prompts.get(2);
        Prompt biographyFollowUp = prompts.get(4);
        assertThat(biographyFollowUp.getInstructions().subList(0, 2))
                .isEqualTo(originalBiography.getInstructions().subList(0, 2));
        assertThat(biographyFollowUp.getInstructions().get(2)).isInstanceOf(AssistantMessage.class);
        assertThat(biographyFollowUp.getInstructions().get(2).getText()).isEqualTo("Invalid biography response");
        assertThat(prompts.get(5).getInstructions()).hasSize(3).noneMatch(AssistantMessage.class::isInstance);
        assertThat(prompts.get(5).getInstructions().get(1).getText())
                .isEqualTo("Confirmed Spotify artist: " + JsonConverter.toJson(found().artist()));
        assertThat(task.getRawRequests()).hasSize(6);
        assertThat(cache).hasSize(5);
    }

    @Test
    void shouldReuseSuccessfulStagesAfterLaterStageFails() {
        Artist artist = artist();
        queuedResponses.add(response);
        queuedResponses.add(response);
        queuedResponses.add("Invalid biography response");
        queuedResponses.add("Invalid biography response");

        assertThatThrownBy(() -> service.discover(task(artist), true)).isInstanceOf(RuntimeException.class);
        assertThat(cache).hasSize(2);
        assertThat(modelCalls).isEqualTo(6);

        assertThat(service.discover(task(artist), true)).isEqualTo(found());

        assertThat(modelCalls).isEqualTo(9);
        assertThat(cache).hasSize(5);
        assertThat(prompts.get(6).getInstructions().getFirst().getText()).contains("biography");
    }

    @Test
    void shouldInvalidateOnlyTheChangedStagePrompt() throws IOException {
        Artist artist = artist();
        assertThat(service.discover(task(artist), true)).isEqualTo(found());
        String changedBiographyPrompt = new ClassPathResource("prompts/spotify-artist-biography.txt").getContentAsString(UTF_8)
                + "\nExtra biography instruction";
        SpotifyArtistDataService changedService = createService(configuredClient,
                new ClassPathResource("prompts/spotify-artist-identity.txt"),
                new ByteArrayResource(changedBiographyPrompt.getBytes(UTF_8)));

        assertThat(changedService.discover(task(artist), true)).isEqualTo(found());

        assertThat(modelCalls).isEqualTo(6);
        assertThat(cache).hasSize(6);
        assertThat(prompts.get(5).getInstructions().getFirst().getText()).isEqualTo(changedBiographyPrompt);
    }

    @Test
    void shouldStopBetweenStagesWhenDiscoveryIsCancelled() {
        when(cacheService.put(eq(SPOTIFY), anyString(), eq(1), anyString())).thenAnswer(invocation -> {
            cache.put(invocation.getArgument(1), invocation.getArgument(3));
            jobSynchronizer.cancelDiscovery();
            return null;
        });

        assertThatThrownBy(() -> service.discover(task(artist()), true)).isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(modelCalls).isEqualTo(1);
        assertThat(cache).hasSize(1);
    }

    @Test
    void shouldPreserveCompleteBiographyWithItsOriginalLanguageAndWhitespace() {
        SpotifyArtistData data = found();
        String biography = "Арам Хачатурян — «Композитор»\n\nOriginal wording: \"About\".\n日本語の紹介\n".repeat(1000);
        SpotifyArtistData longBiography = new SpotifyArtistData(data.artist(), data.matchedAlbum(), data.topTracks(),
                data.similarArtists(), biography, data.links(), data.imageUrl(), data.avatarUrl());
        response = spotifyArtistResponse(longBiography);

        assertThat(service.discover(task(artist()), true)).isEqualTo(longBiography);
        assertThat(service.discover(task(artist()), true).biography()).isEqualTo(biography);
    }

    @Test
    void shouldFormatOnlyMalformedStageBeforeContinuing() {
        queuedResponses.add(response);
        queuedResponses.add(response);
        queuedResponses.add("{\"biography\":\"Artist biography\"");
        queuedFormattingResponses.add(response);
        DiscoveryTask task = task(artist());

        assertThat(service.discover(task, true)).isEqualTo(found());

        assertThat(modelCalls).isEqualTo(6);
        assertThat(prompts).filteredOn(prompt -> requireNonNull(requireNonNull(
                ((OpenAiChatOptions) prompt.getOptions())).getResponseFormat()).getType() == ResponseFormat.Type.JSON_SCHEMA)
                .singleElement().satisfies(prompt -> {
                    assertThat(((OpenAiChatOptions) prompt.getOptions()).getToolChoice()).isEqualTo("none");
                    assertThat(prompt.getInstructions().getLast().getText()).isEqualTo("{\"biography\":\"Artist biography\"");
                });
        assertThat(task.getRawRequests()).hasSize(5);
        assertThat(task.getRawResponses()).hasSize(5);
        assertThat(cache).hasSize(5);
        verifyNoInteractions(logService);
    }

    @Test
    void shouldAcceptIdentityWhenNullableFieldIsMissing() {
        Map<String, Object> identity = new LinkedHashMap<>(JsonConverter.mapFromJson(response, String.class, Object.class));
        identity.remove("avatarUrl");
        queuedResponses.add(JsonConverter.toJson(identity));
        SpotifyArtistData expected = found();

        assertThat(service.discover(task(artist()), true)).isEqualTo(new SpotifyArtistData(expected.artist(),
                expected.matchedAlbum(), expected.topTracks(), expected.similarArtists(), expected.biography(),
                expected.links(), expected.imageUrl(), null));

        assertThat(modelCalls).isEqualTo(5);
        assertThat(prompts).allSatisfy(prompt -> assertThat(
                requireNonNull(requireNonNull(((OpenAiChatOptions) prompt.getOptions())).getResponseFormat()).getType())
                .isEqualTo(ResponseFormat.Type.TEXT));
        assertThat(cache).hasSize(5);
        verifyNoInteractions(logService);
    }

    @ParameterizedTest
    @MethodSource("invalidPopularTracks")
    void shouldRejectInvalidPopularTracks(List<SpotifyArtistData.TopTrack> tracks) {
        SpotifyArtistData data = found();
        response = spotifyArtistResponse(new SpotifyArtistData(data.artist(), data.matchedAlbum(), tracks,
                data.similarArtists(), data.biography(), data.links(), data.imageUrl(), data.avatarUrl()));

        assertThatThrownBy(() -> service.discover(task(artist()), true)).isInstanceOf(RuntimeException.class);

        assertThat(cache).hasSize(1);
        assertThat(cache.keySet()).allMatch(key -> key.startsWith("SPOTIFY_ARTIST_IDENTITY:"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldRejectDuplicateOrSelfSimilarArtists(boolean self) {
        SpotifyArtistData data = found();
        List<SpotifyArtistData.SpotifyArtist> artists = self ? List.of(data.artist())
                : List.of(data.similarArtists().getFirst(), data.similarArtists().getFirst());
        response = spotifyArtistResponse(new SpotifyArtistData(data.artist(), data.matchedAlbum(), data.topTracks(),
                artists, data.biography(), data.links(), data.imageUrl(), data.avatarUrl()));

        assertThatThrownBy(() -> service.discover(task(artist()), true)).isInstanceOf(RuntimeException.class);

        assertThat(cache).hasSize(4);
        assertThat(cache.keySet()).noneMatch(key -> key.startsWith("SPOTIFY_ARTIST_SIMILAR_ARTISTS:"));
    }

    @ParameterizedTest
    @MethodSource("missingStageFields")
    void shouldRequestStageLocalVerificationWhenOptionalFieldIsOmitted(int stageIndex, String incompleteResponse) {
        for (int i = 0; i < stageIndex; i++) {
            queuedResponses.add(response);
        }
        queuedResponses.add(incompleteResponse);

        assertThat(service.discover(task(artist()), true)).isEqualTo(found());

        assertThat(modelCalls).isEqualTo(7);
        assertThat(cache).hasSize(5);
        Prompt originalStage = prompts.get(stageIndex);
        Prompt followUp = prompts.get(stageIndex + 2);
        assertThat(followUp.getInstructions().subList(0, 2))
                .isEqualTo(originalStage.getInstructions().subList(0, 2));
        assertThat(followUp.getInstructions().get(2)).isInstanceOf(AssistantMessage.class);
        assertThat(followUp.getInstructions().get(2).getText()).isEqualTo(incompleteResponse);
    }

    @ParameterizedTest
    @MethodSource("missingStageFields")
    void shouldRejectAndNotCacheOptionalStageWithRepeatedlyOmittedField(int stageIndex, String incompleteResponse) {
        for (int i = 0; i < stageIndex; i++) {
            queuedResponses.add(response);
        }
        queuedResponses.add(incompleteResponse);
        queuedResponses.add(incompleteResponse);

        assertThatThrownBy(() -> service.discover(task(artist()), true)).isInstanceOf(RuntimeException.class);

        assertThat(modelCalls).isEqualTo(stageIndex + 4);
        assertThat(cache).hasSize(stageIndex);
    }

    @Test
    void shouldPropagateConfiguredClientFailureWithoutCaching() {
        when(model.call(any(Prompt.class))).thenThrow(new IllegalStateException("Playwright is unavailable"));

        assertThatThrownBy(() -> service.discover(task(artist()), true)).hasMessageContaining("Playwright is unavailable");
        assertThat(cache).isEmpty();
        verify(model).call(any(Prompt.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Here is the result:\n%s", "%s\nDone.", "```json\n%s\n```",
            "Here is the result:\n```json\n%s\n```\nDone."})
    void shouldExtractJsonWithoutRequestingVerification(String responseTemplate) {
        response = responseTemplate.formatted(response);
        Artist artist = artist();
        DiscoveryTask task = task(artist);

        assertThat(service.discover(task, true)).isEqualTo(found());
        assertThat(service.discover(task(artist), true)).isEqualTo(found());

        assertThat(modelCalls).isEqualTo(5);
        assertThat(task.getRawResponses()).containsExactlyElementsOf(
                collectedResponses.stream().map(JsonConverter::fromJsonOrString).toList());
        verifyNoInteractions(logService);
        Map<?, ?> entry = cacheEntry("SPOTIFY_ARTIST_IDENTITY");
        assertThat(entry.get("response")).isEqualTo(collectedResponses.getFirst());
    }

    @ParameterizedTest
    @MethodSource("invalidResponsesRequiringVerification")
    void shouldRequestVerificationAndWarnAboutInvalidResponse(String invalidResponseTemplate, boolean requiresFormatting) throws IOException {
        configuredClient = ChatClient.builder(model).defaultTools(tool("browser_navigate")).build();
        service = createService(configuredClient, new ClassPathResource("prompts/spotify-artist-identity.txt"));
        String invalidResponse = invalidResponseTemplate != null ? invalidResponseTemplate.formatted(response) : null;
        queuedResponses.add(invalidResponse);
        Artist artist = artist();
        DiscoveryTask task = task(artist);

        assertThat(service.discover(task, true)).isEqualTo(found());

        assertThat(modelCalls).isEqualTo(requiresFormatting ? 7 : 6);
        assertThat(task.getRawResponses()).hasSize(6).containsExactlyElementsOf(
                collectedResponses.stream().map(JsonConverter::fromJsonOrString).toList());
        Prompt verification = prompts.get(requiresFormatting ? 2 : 1);
        assertThat(verification.getInstructions().subList(0, 2)).isEqualTo(prompts.getFirst().getInstructions().subList(0, 2));
        if (invalidResponse != null && !invalidResponse.isBlank()) {
            assertThat(verification.getInstructions().get(2)).isInstanceOf(AssistantMessage.class);
            assertThat(verification.getInstructions().get(2).getText()).isEqualTo(invalidResponse);
        }
        String verificationPrompt = verification.getInstructions().get(verification.getInstructions().size() - 2).getText();
        assertThat(verificationPrompt).contains("current stage", "all URLs");
        assertThat(requireNonNull(((ToolCallingChatOptions) verification.getOptions())).getToolCallbacks())
                .extracting(callback -> callback.getToolDefinition().name()).containsExactly("browser_navigate");
        verify(logService).warn(any(), eq("Invalid Spotify response from the LLM for artist '{}'."),
                eq(artist.getName()), any(RuntimeException.class));
        assertThat(cache).hasSize(5);
        Map<?, ?> entry = cacheEntry("SPOTIFY_ARTIST_IDENTITY");
        assertThat(entry.get("response")).isEqualTo(collectedResponses.get(1));
        assertThat(((Map<?, ?>) entry.get("request")).get("userPrompt")).isEqualTo(verificationPrompt);
    }

    private static Stream<Arguments> invalidResponsesRequiringVerification() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of("", false),
                Arguments.of("not JSON", true),
                Arguments.of("{}", false),
                Arguments.of("{\"status\":\"FOUND\"}", false),
                Arguments.of("Result: {status: 'FOUND'}", true),
                Arguments.of("Result: {\"status\":\"UNKNOWN\"}", true),
                Arguments.of("Result: {\"status\":\"FOUND\"}", false));
    }

    @Test
    void shouldSkipCacheForBothAttemptsWhenDisabled() {
        queuedResponses.add("Invalid response");

        assertThat(service.discover(task(artist()), false)).isEqualTo(found());

        assertThat(modelCalls).isEqualTo(7);
        verifyNoInteractions(cacheService);
    }

    @Test
    void shouldNotVerifyWhenDiscoveryIsCancelledAfterInvalidResponse() {
        response = "Invalid response";
        doAnswer(invocation -> {
            jobSynchronizer.cancelDiscovery();
            return Optional.empty();
        }).when(logService).warn(any(), anyString(), any(), any());

        assertThatThrownBy(() -> service.discover(task(artist()), true)).isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(modelCalls).isEqualTo(2);
        assertThat(cache).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "null", "not JSON"})
    void shouldNotCacheInvalidJson(String invalidResponse) {
        response = invalidResponse;
        Artist artist = artist();
        assertThatThrownBy(() -> service.discover(task(artist), true)).isInstanceOf(RuntimeException.class);
        assertThat(cache).isEmpty();
        assertThat(modelCalls).isEqualTo(invalidResponse == null || invalidResponse.isBlank() ? 2 : 4);
        verify(logService, times(2)).warn(any(), eq("Invalid Spotify response from the LLM for artist '{}'."),
                eq(artist.getName()), any(RuntimeException.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOT_FOUND", "AMBIGUOUS"})
    void shouldCacheUnconfirmedArtists(String status) {
        Artist artist = artist();
        response = spotifyArtistResponse(status);

        assertThat(service.discover(task(artist), true)).isNull();
        assertThat(service.discover(task(artist), true)).isNull();

        assertThat(cache).hasSize(1);
        assertThat(modelCalls).isEqualTo(1);
    }

    @Test
    void shouldRejectAlbumThatWasNotSupplied() {
        SpotifyArtistData data = found();
        response = spotifyArtistResponse(new SpotifyArtistData(data.artist(),
                new SpotifyArtistData.MatchedAlbum("Other album", "Album", ALBUM_ID, albumUrl()),
                data.topTracks(), data.similarArtists(), data.biography(), data.links(), data.imageUrl(), data.avatarUrl()));
        assertThatThrownBy(() -> service.discover(task(artist()), true)).hasMessageContaining("invalid Spotify response");
        assertThat(cache).isEmpty();
        assertThat(modelCalls).isEqualTo(2);
        assertThat(prompts).allSatisfy(prompt -> assertThat(
                requireNonNull(requireNonNull(((OpenAiChatOptions) prompt.getOptions())).getResponseFormat()).getType())
                .isEqualTo(ResponseFormat.Type.TEXT));
    }

    @Test
    void shouldAcceptAndCacheDifferentSpotifyUrlAndIdFormats() {
        String albumUrl = "https://open.spotify.com/intl-de/release/album-id-v2?si=example";
        SpotifyArtistData data = new SpotifyArtistData(
                new SpotifyArtistData.SpotifyArtist("artist-id-v2", "Artist",
                        "https://artists.spotify.com/profile/artist-id-v2?locale=de"),
                new SpotifyArtistData.MatchedAlbum("Album", "Album", "album-id-v2", albumUrl),
                List.of(new SpotifyArtistData.TopTrack("track-id-v2", "Track",
                        "https://open.spotify.com/intl-de/song/track-id-v2?si=example#details",
                        "Album", "album-id-v2", albumUrl)),
                List.of(new SpotifyArtistData.SpotifyArtist("related-id-v2", "Related artist",
                        "https://open.spotify.com/intl-de/artist/related-id-v2?si=example")),
                null, null, null, null);
        response = spotifyArtistResponse(data);
        Artist artist = artist();

        assertThat(service.discover(task(artist), true)).isEqualTo(data);
        assertThat(service.discover(task(artist), true)).isEqualTo(data);
        assertThat(modelCalls).isEqualTo(5);
    }

    @Test
    void shouldRequireArtistAndMatchedAlbumInStoredData() {
        SpotifyArtistData data = found();

        assertThat(validator.validate(new SpotifyArtistData(null, null, null, null, null, null, null, null)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("artist", "matchedAlbum");
        assertThat(validator.validate(new SpotifyArtistData(data.artist(), data.matchedAlbum(),
                null, null, null, null, null, null))).isEmpty();
    }

    @Test
    void shouldAllowUnavailableOptionalSections() {
        SpotifyArtistData data = found();
        SpotifyArtistData partial = new SpotifyArtistData(data.artist(), data.matchedAlbum(),
                null, null, null, null, null, null);
        response = spotifyArtistResponse(partial);

        assertThat(service.discover(task(artist()), true)).isEqualTo(partial);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldAcceptAndCacheImageAndAvatarIndependently(boolean hasAvatar) {
        SpotifyArtistData data = found();
        SpotifyArtistData partial = new SpotifyArtistData(data.artist(), data.matchedAlbum(),
                data.topTracks(), data.similarArtists(), data.biography(), data.links(),
                hasAvatar ? null : data.imageUrl(), hasAvatar ? data.avatarUrl() : null);
        response = spotifyArtistResponse(partial);
        Artist artist = artist();

        assertThat(service.discover(task(artist), true)).isEqualTo(partial);
        assertThat(service.discover(task(artist), true)).isEqualTo(partial);
        assertThat(modelCalls).isEqualTo(5);
    }

    @Test
    void shouldReadStoredArtistDataWithoutAvatarUrl() {
        Map<String, Object> legacyData = new HashMap<>(JsonConverter.mapFromJson(JsonConverter.toJson(found()), String.class, Object.class));
        legacyData.remove("avatarUrl");

        SpotifyArtistData data = JsonConverter.fromJson(JsonConverter.toJson(legacyData), SpotifyArtistData.class);

        assertThat(data.avatarUrl()).isNull();
        assertThat(data.imageUrl()).isEqualTo(found().imageUrl());
    }

    @ParameterizedTest
    @MethodSource("invalidResponses")
    void shouldRejectInvalidFieldsOrConfirmedArtistWithoutIdentification(String invalidResponse) {
        response = invalidResponse;
        assertThatThrownBy(() -> service.discover(task(artist()), true)).hasMessageContaining("invalid Spotify response");
        assertThat(cache).isEmpty();
    }

    @Test
    void shouldNotCallLlmOnShutdown() {
        shutdownService.onApplicationEvent();

        assertThatThrownBy(() -> service.discover(task(artist()), true)).isInstanceOf(DiscoveryInterruptedException.class);
        assertThat(cache).isEmpty();
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void shouldInterruptWithoutCachingWhenShutdownStartsDuringLlmCall() {
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            shutdownService.onApplicationEvent();
            return new ChatResponse(List.of(new Generation(new AssistantMessage(response))));
        });

        assertThatThrownBy(() -> service.discover(task(artist()), true)).isInstanceOf(DiscoveryInterruptedException.class);
        assertThat(cache).isEmpty();
        verify(model).call(any(Prompt.class));
    }

    @Test
    void shouldNotCallLlmWhenShutdownStartsWhileReadingCache() {
        when(cacheService.get(eq(SPOTIFY), anyString(), eq(1))).thenAnswer(invocation -> {
            shutdownService.onApplicationEvent();
            return Optional.empty();
        });

        assertThatThrownBy(() -> service.discover(task(artist()), true)).isInstanceOf(DiscoveryInterruptedException.class);
        assertThat(cache).isEmpty();
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void shouldNotExecuteToolsWhenShutdownStartsDuringLlmCall() throws IOException {
        ToolCallback callback = tool("browser_navigate");
        configuredClient = ChatClient.builder(model).defaultTools(callback).build();
        service = createService(configuredClient, new ClassPathResource("prompts/spotify-artist-identity.txt"));
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            shutdownService.onApplicationEvent();
            return toolCallResponse();
        });

        assertThatThrownBy(() -> service.discover(task(artist()), true)).isInstanceOf(DiscoveryInterruptedException.class);
        assertThat(cache).isEmpty();
        verify(model).call(any(Prompt.class));
        verify(callback, never()).call(anyString(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void shouldStopBeforeNextLlmIterationWhenShutdownStartsInTool(int toolCallsBeforeShutdown) throws IOException {
        ToolCallback callback = tool("browser_navigate");
        when(callback.getToolMetadata()).thenReturn(ToolMetadata.builder().build());
        AtomicInteger toolCalls = new AtomicInteger();
        when(callback.call(anyString(), any())).thenAnswer(invocation -> {
            if (toolCalls.incrementAndGet() == toolCallsBeforeShutdown) {
                shutdownService.onApplicationEvent();
            }
            return "Spotify page loaded";
        });
        configuredClient = ChatClient.builder(model).defaultTools(callback).build();
        service = createService(configuredClient, new ClassPathResource("prompts/spotify-artist-identity.txt"));
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            modelCalls++;
            return modelCalls <= toolCallsBeforeShutdown ? toolCallResponse()
                    : new ChatResponse(List.of(new Generation(new AssistantMessage(response))));
        });

        assertThatThrownBy(() -> service.discover(task(artist()), true)).isInstanceOf(DiscoveryInterruptedException.class);
        assertThat(cache).isEmpty();
        verify(model, times(toolCallsBeforeShutdown)).call(any(Prompt.class));
        verify(callback, times(toolCallsBeforeShutdown)).call(anyString(), any());
    }

    @Test
    void shouldPreserveAlbumListAndEscapeNamesAsData() {
        Artist artist = artist("Artist {name}",
                new Album().setName(null), new Album().setName("Album\n\"Quoted\"").setYear(2000));
        response = spotifyArtistResponse("NOT_FOUND");

        service.discover(task(artist), true);

        assertThat(prompts.getFirst().getInstructions().get(1).getText())
                .contains("\"Artist {name}\"", "- null", "- \"Album\\n\\\"Quoted\\\"\" (year: 2000)");
    }

    @Test
    void shouldSortAlbumsUsingTheirNaturalOrderAndReuseCacheRegardlessOfInputOrder() {
        Album earlier = new Album().setName("Z album").setYear(2000);
        Album later = new Album().setName("Album").setYear(2010);
        Artist artist = artist("Artist", later, earlier);

        service.discover(task(artist), true);
        artist.setAlbums(List.of(earlier, later));
        service.discover(task(artist), true);

        assertThat(prompts.getFirst().getInstructions().get(1).getText())
                .contains("- \"Z album\" (year: 2000)\n- \"Album\" (year: 2010)");
        assertThat(cache).hasSize(5);
        assertThat(modelCalls).isEqualTo(5);
    }

    @Test
    void shouldSkipArtistWithoutAlbums() {
        Artist artist = artist("Artist");

        assertThat(service.discover(task(artist), true)).isNull();

        verifyNoInteractions(logService);
        verify(model, never()).call(any(Prompt.class));
        verifyNoInteractions(cacheService);
    }

    @Test
    void shouldSkipArtistWithoutName() {
        Artist artist = artist(null, new Album().setName("Album"));

        assertThat(service.discover(task(artist), true)).isNull();

        verifyNoInteractions(logService);
        verify(model, never()).call(any(Prompt.class));
        verifyNoInteractions(cacheService);
    }

    private SpotifyArtistDataService createService(ChatClient client, Resource promptResource) throws IOException {
        return createService(client, promptResource, new ClassPathResource("prompts/spotify-artist-biography.txt"));
    }

    private SpotifyArtistDataService createService(ChatClient client, Resource promptResource, Resource biographyPromptResource) throws IOException {
        DiscoveryChatClient discoveryClient = new DiscoveryChatClient((customizer, toolsEnabled) -> client,
                jobSynchronizer, new DiscoveryAdvisor(jobSynchronizer));
        DiscoveryTaskLlmExecutor operation = new DiscoveryTaskLlmExecutor(discoveryClient, cacheService,
                new DiscoveryTaskExchangeRecorder(discoveryTaskRepository, transactionManager()),
                logService, jobSynchronizer, 1, 1, Duration.ofMinutes(10));
        return new SpotifyArtistDataService(operation,
                validator, artistRepository, logService,
                jobSynchronizer, transactionManager(), promptResource,
                new ClassPathResource("prompts/spotify-artist-stage-verification.txt"),
                new ClassPathResource("prompts/spotify-artist-popular-tracks.txt"),
                biographyPromptResource,
                new ClassPathResource("prompts/spotify-artist-about-media.txt"),
                new ClassPathResource("prompts/spotify-artist-similar-artists.txt"));
    }

    private String stageResponse(Prompt prompt, String suppliedResponse) {
        if (suppliedResponse == null || suppliedResponse.isBlank()) {
            return suppliedResponse;
        }
        Object parsed;
        try {
            parsed = LlmJsonConverter.fromJson(suppliedResponse);
        } catch (RuntimeException e) {
            return suppliedResponse;
        }
        if (!(parsed instanceof Map<?, ?> values)) {
            return suppliedResponse;
        }
        OpenAiChatOptions options = (OpenAiChatOptions) prompt.getOptions();
        ResponseFormat responseFormat = requireNonNull(requireNonNull(options).getResponseFormat());
        Map<?, ?> schema = responseFormat.getType() == ResponseFormat.Type.TEXT
                ? collectionSchema(prompt) : JsonConverter.fromJson(responseFormat.getJsonSchema(), Map.class);
        Map<?, ?> properties = (Map<?, ?>) schema.get("properties");
        Map<String, Object> stageValues = new LinkedHashMap<>();
        properties.keySet().forEach(key -> {
            if (values.containsKey(key)) {
                stageValues.put(key.toString(), values.get(key));
            }
        });
        int start = suppliedResponse.indexOf('{');
        int end = suppliedResponse.lastIndexOf('}');
        return suppliedResponse.substring(0, start) + JsonConverter.toJson(stageValues) + suppliedResponse.substring(end + 1);
    }

    private Map<?, ?> cacheEntry(String prefix) {
        String value = cache.entrySet().stream().filter(entry -> entry.getKey().startsWith(prefix + ":"))
                .findFirst().orElseThrow().getValue();
        return JsonConverter.fromJson(value, Map.class);
    }

    private Artist artist() {
        return artist("Artist", new Album().setName("Album"));
    }

    private DiscoveryTask task(Artist artist) {
        return new DiscoveryTask()
                .setType(DiscoveryTaskType.SPOTIFY_ARTIST_DATA)
                .setArtist(artist);
    }

    private Artist artist(String name, Album... albums) {
        Artist artist = new Artist().setId("artist-" + artistCount++).setName(name).setAlbums(List.of(albums));
        artist.getAlbums().forEach(album -> album.setArtist(artist));
        lenient().when(artistRepository.findById(artist.getId())).thenReturn(Optional.of(artist));
        return artist;
    }

    private static SpotifyArtistData found() {
        return new SpotifyArtistData(
                new SpotifyArtistData.SpotifyArtist(ARTIST_ID, "Artist", "https://open.spotify.com/artist/" + ARTIST_ID),
                new SpotifyArtistData.MatchedAlbum("Album", "Album", ALBUM_ID, albumUrl()),
                List.of(new SpotifyArtistData.TopTrack("c".repeat(22), "Top track",
                        "https://open.spotify.com/track/" + "c".repeat(22), "Album", ALBUM_ID, albumUrl())),
                List.of(new SpotifyArtistData.SpotifyArtist("d".repeat(22), "Related artist",
                        "https://open.spotify.com/artist/" + "d".repeat(22))),
                "Artist biography", List.of(new SpotifyArtistData.ExternalLink("Website", "https://example.com/")),
                "https://i.scdn.co/image/artist", "https://i.scdn.co/image/avatar");
    }

    private static String albumUrl() {
        return "https://open.spotify.com/album/" + ALBUM_ID;
    }

    private static Stream<Arguments> missingStageFields() {
        return Stream.of(
                Arguments.of(1, "{}"),
                Arguments.of(2, "{}"),
                Arguments.of(3, "{}"),
                Arguments.of(4, "{}"),
                Arguments.of(3, "{\"links\":null}"),
                Arguments.of(3, "{\"imageUrl\":null}"));
    }

    private static Stream<List<SpotifyArtistData.TopTrack>> invalidPopularTracks() {
        SpotifyArtistData.TopTrack track = found().topTracks().getFirst();
        List<SpotifyArtistData.TopTrack> tooMany = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            tooMany.add(new SpotifyArtistData.TopTrack("track-" + i, track.title(), track.url(),
                    track.albumTitle(), track.albumId(), track.albumUrl()));
        }
        return Stream.of(tooMany, List.of(track, track),
                List.of(new SpotifyArtistData.TopTrack("", track.title(), track.url(),
                        track.albumTitle(), track.albumId(), track.albumUrl())));
    }

    private static Stream<String> invalidResponses() {
        SpotifyArtistData data = found();
        return Stream.of(
                "{\"status\":null}",
                spotifyArtistResponse(new SpotifyArtistData(null, data.matchedAlbum(), null, null, null, null, null, null)),
                spotifyArtistResponse(new SpotifyArtistData(data.artist(), null, null, null, null, null, null, null)),
                spotifyArtistResponse(new SpotifyArtistData(new SpotifyArtistData.SpotifyArtist("", "Artist", "url"),
                        data.matchedAlbum(), null, null, null, null, null, null)));
    }

    private ChatResponse toolCallResponse() {
        return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                .toolCalls(List.of(new AssistantMessage.ToolCall("tool-call", "function", "browser_navigate", "{}")))
                .build())));
    }

    private ToolCallback tool(String name) {
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name(name).description(name).inputSchema("{\"type\":\"object\"}").build());
        return callback;
    }
}
