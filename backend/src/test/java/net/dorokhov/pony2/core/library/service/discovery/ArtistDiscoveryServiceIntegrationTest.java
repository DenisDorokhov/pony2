package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.config.service.command.ConfigSetUpdateCommand;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import net.dorokhov.pony2.core.llm.repository.DiscoveryCacheRepository;
import net.dorokhov.pony2.core.log.repository.LogMessageRepository;
import net.dorokhov.pony2.core.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static net.dorokhov.pony2.test.SongFixtures.song;
import static net.dorokhov.pony2.test.UserFixtures.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ArtistDiscoveryServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ArtistDiscoveryService service;
    @Autowired
    private ConfigService configService;
    @Autowired
    private DiscoveryTaskExecutor taskExecutor;
    @Autowired
    private LibraryJobSynchronizer jobSynchronizer;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private DiscoveryJobRepository jobRepository;
    @Autowired
    private ArtistDiscoveryRepository artistDiscoveryRepository;
    @Autowired
    private ArtistDiscoveryIndexRepository artistDiscoveryIndexRepository;
    @Autowired
    private DiscoveryTaskRepository taskRepository;
    @Autowired
    private DiscoveryJobInterruptionService interruptionService;
    @Autowired
    private DiscoveryCacheRepository cacheRepository;
    @Autowired
    private LogMessageRepository logRepository;

    private LibraryJobSynchronizer.LibraryJobRegistration jobRegistration;

    @AfterEach
    void tearDown() {
        jobRegistration.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        jobRegistration = jobSynchronizer.registerDiscoveryJob();
        userRepository.save(user().setEmail("admin@example.com").setRoles(Set.of(User.Role.ADMIN)));
        configService.update(new ConfigSetUpdateCommand()
                .setLlmUrl("http://localhost:11434/v1")
                .setLlmModel("test"));
        lenient().when(model.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCommitStartedTaskBeforeLlmAndPersistCompletedResult(boolean cacheEnabled) {
        Artist artist = saveArtist("Album");
        DiscoveryJob job = saveJob();
        List<DiscoveryProgress> progress = new ArrayList<>();
        SpotifyArtistData result = new SpotifyArtistData(SpotifyArtistData.Status.NOT_FOUND,
                null, null, null, null, "Biography".repeat(1000), null, null);
        String rawResponse = "\n " + JsonConverter.toJson(result) + "\n";
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            Prompt prompt = invocation.getArgument(0);
            assertThat(prompt.getInstructions().get(1).getText()).contains("- \"Album\"");
            assertThat(taskRepository.findAll()).singleElement()
                    .satisfies(task -> {
                        assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.STARTED);
                        RawRequest rawRequest = JsonConverter.fromJson(task.getRawRequest(), RawRequest[].class)[0];
                        assertThat(rawRequest.systemPrompt()).isEqualTo(prompt.getInstructions().getFirst().getText());
                        assertThat(rawRequest.userPrompt()).isEqualTo(prompt.getInstructions().get(1).getText());
                        assertThat(rawRequest.albumTitles()).isEqualTo(List.of("Album"));
                        assertThat(task.getRawResult()).isEqualTo("[null]");
                    });
            getTransactionTemplate().executeWithoutResult(status -> {
                ArtistDiscovery discovery = artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc(artist.getId()).orElseThrow();
                assertThat(discovery.getTasks()).singleElement()
                        .satisfies(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.STARTED));
            });
            return new ChatResponse(List.of(new Generation(new AssistantMessage(rawResponse))));
        });

        service.discover(job, artist, cacheEnabled, value -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(taskRepository.countByJobIdAndStatus(job.getId(), DiscoveryTask.Status.COMPLETE))
                    .isEqualTo(value.getValue().getItemsComplete());
            progress.add(value);
        });
        assertThat(cacheRepository.count()).isEqualTo(cacheEnabled ? 1 : 0);
        assertThat(progress).allSatisfy(value -> assertThat(value.getStep())
                .isEqualTo(DiscoveryProgress.Step.ARTIST_DISCOVERY));
        assertThat(progress).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 3), DiscoveryProgress.Value.of(1, 3), DiscoveryProgress.Value.of(2, 3), DiscoveryProgress.Value.of(3, 3));

        getTransactionTemplate().executeWithoutResult(status -> {
            ArtistDiscovery discovery = artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc(artist.getId()).orElseThrow();
            assertThat(discovery.getTasks()).hasSize(2);
            assertThat(discovery.getTasks().stream().filter(task -> task.getType() == DiscoveryTaskType.SPOTIFY_ARTIST_DATA))
                    .singleElement().satisfies(task -> {
                        assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
                        assertThat(task.getType()).isEqualTo(DiscoveryTaskType.SPOTIFY_ARTIST_DATA);
                        assertThat(task.getJob().getId()).isEqualTo(job.getId());
                        assertThat(task.getParameter()).isEqualTo(JsonConverter.toJson(new DiscoveryTask.ArtistParameter(artist.getId())));
                        assertThat(JsonConverter.fromJson(task.getResult(), SpotifyArtistData.class)).isEqualTo(result);
                        assertThat(task.getRawRequest()).isNotBlank();
                        assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(rawResponse);
                    });
        });
        assertEmptyTopTracks();
    }

    @Test
    void shouldDiscoverAlbumsWhenLlmIsDisabled() {
        configService.update(new ConfigSetUpdateCommand());
        List<DiscoveryProgress> progress = new ArrayList<>();

        service.discover(saveJob(), saveArtist("Album"), true, progress::add);

        assertThat(progress).allSatisfy(value -> {
            assertThat(value.getStep()).isEqualTo(DiscoveryProgress.Step.ARTIST_DISCOVERY);
        });
        assertThat(progress).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 1), DiscoveryProgress.Value.of(1, 1));
        assertThat(taskRepository.findAll()).singleElement().satisfies(task -> {
            assertThat(task.getType()).isEqualTo(DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA);
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        });
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void shouldUpdateSpotifyArtistIdInDiscoveryIndex() {
        Artist artist = saveArtist("Album");
        when(model.call(any(Prompt.class))).thenReturn(chatResponse(spotifyArtistData("first-spotify-artist")),
                chatResponse(spotifyArtistData("second-spotify-artist")));

        service.discover(saveJob(), artist, false, null);
        ArtistDiscoveryIndex original = artistDiscoveryIndexRepository.findByArtistId(artist.getId()).orElseThrow();
        assertThat(original.getSpotifyArtistId()).isEqualTo("first-spotify-artist");

        service.discover(saveJob(), artist, false, null);

        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
            assertThat(index.getId()).isEqualTo(original.getId());
            assertThat(index.getCreationDate()).isEqualTo(original.getCreationDate());
            assertThat(index.getUpdateDate()).isNotNull();
            assertThat(index.getSpotifyArtistId()).isEqualTo("second-spotify-artist");
        });
        assertThat(artistDiscoveryIndexRepository.count()).isOne();
        assertThat(artistDataTasks()).hasSize(2)
                .allSatisfy(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE));
    }

    @Test
    void shouldRestoreDiscoveryIndexFromCachedSpotifyArtistData() {
        Artist artist = saveArtist("Album");
        when(model.call(any(Prompt.class))).thenReturn(chatResponse(spotifyArtistData("spotify-artist")));
        service.discover(saveJob(), artist, true, null);
        ArtistDiscoveryIndex index = artistDiscoveryIndexRepository.findByArtistId(artist.getId()).orElseThrow();
        artistDiscoveryIndexRepository.deleteById(index.getId());
        clearInvocations(model);
        List<DiscoveryProgress> progress = new ArrayList<>();

        service.discover(saveJob(), artist, true, progress::add);

        assertThat(progress).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 3), DiscoveryProgress.Value.of(1, 3), DiscoveryProgress.Value.of(2, 3), DiscoveryProgress.Value.of(3, 3));
        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get()
                .extracting(ArtistDiscoveryIndex::getSpotifyArtistId).isEqualTo("spotify-artist");
        assertThat(artistDiscoveryIndexRepository.count()).isOne();
        verify(model, never()).call(any(Prompt.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldRollBackCompletedTaskAndIndexWhenIndexSavingFails(boolean existingIndex) {
        Artist artist = saveArtist("Album");
        ArtistDiscoveryIndex previousIndex = existingIndex ? saveIndex(artist, "previous-spotify-artist") : null;
        // Pass response validation but exceed the database column length to force a transaction rollback.
        SpotifyArtistData result = spotifyArtistData("x".repeat(256));
        when(model.call(any(Prompt.class))).thenReturn(chatResponse(result));

        service.discover(saveJob(), artist, false, null);

        assertThat(artistDataTasks()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
            assertThat(task.getResult()).contains("Value too long", "SPOTIFY_ARTIST_ID");
        });
        if (previousIndex == null) {
            assertThat(artistDiscoveryIndexRepository.count()).isZero();
        } else {
            assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
                assertThat(index.getId()).isEqualTo(previousIndex.getId());
                assertThat(index.getSpotifyArtistId()).isEqualTo("previous-spotify-artist");
            });
            assertThat(artistDiscoveryIndexRepository.count()).isOne();
        }
    }

    @Test
    void shouldPersistRawExchangeFromCache() {
        Artist artist = saveArtist("Album");
        SpotifyArtistData result = new SpotifyArtistData(SpotifyArtistData.Status.NOT_FOUND,
                null, null, null, null, null, null, null);
        String rawResponse = "\n " + JsonConverter.toJson(result) + "\n";
        when(model.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(rawResponse)))));
        service.discover(saveJob(), artist, true, null);
        DiscoveryTask originalTask = artistDataTasks().getFirst();
        clearInvocations(model);

        service.discover(saveJob(), artist, true, null);

        assertThat(taskRepository.findAll()).hasSize(6);
        assertThat(artistDataTasks()).hasSize(2).allSatisfy(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(task.getRawRequest()).isEqualTo(originalTask.getRawRequest());
            assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(rawResponse);
        });
        verify(model, never()).call(any(Prompt.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not JSON", "{\"status\":\"FOUND\"}"})
    void shouldPersistRawExchangeWhenResponseIsInvalid(String rawResponse) {
        Artist artist = saveArtist("Album");
        saveIndex(artist, "previous-spotify-artist");
        when(model.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(rawResponse)))));

        service.discover(saveJob(), artist, true, null);

        assertThat(artistDataTasks()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
            assertThat(task.getRawRequest()).isNotBlank();
            assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class))
                    .containsExactly(rawResponse, rawResponse, rawResponse, rawResponse);
            assertThat(task.getResult()).isNotBlank();
        });
        assertFailedTopTracks();
        assertThat(cacheRepository.count()).isZero();
        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get()
                .extracting(ArtistDiscoveryIndex::getSpotifyArtistId).isEqualTo("previous-spotify-artist");
    }

    @Test
    void shouldReadSpotifyArtistIdFromPreviousTask() {
        Artist artist = saveArtist("Album");
        ArtistDiscovery discovery = artistDiscoveryRepository.save(new ArtistDiscovery()
                .setArtist(artist)
                .setJob(saveJob()));
        SpotifyArtistData artistData = spotifyArtistData("spotify-artist");
        List<String> tracks = List.of("track-1", "track-2");

        executeTask(discovery, DiscoveryTaskType.SPOTIFY_ARTIST_DATA,
                new DiscoveryTask.ArtistParameter(artist.getId()), task -> artistData);
        executeTask(discovery, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, "spotify-artist", task -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            String spotifyArtistId = JsonConverter.fromJson(task.getParameter(), String.class);
            DiscoveryTask previousTask = discovery.getTasks().getFirst();
            assertThat(previousTask.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            SpotifyArtistData previousResult = JsonConverter.fromJson(previousTask.getResult(), SpotifyArtistData.class);
            assertThat(previousResult.artist().id()).isEqualTo(spotifyArtistId);
            return tracks;
        });

        getTransactionTemplate().executeWithoutResult(status -> {
            ArtistDiscovery persistedDiscovery = artistDiscoveryRepository.findById(discovery.getId()).orElseThrow();
            assertThat(persistedDiscovery.getTasks()).extracting(DiscoveryTask::getStatus)
                    .containsOnly(DiscoveryTask.Status.COMPLETE);
            assertThat(persistedDiscovery.getTasks()).extracting(DiscoveryTask::getResult)
                    .containsExactlyInAnyOrder(JsonConverter.toJson(artistData), JsonConverter.toJson(tracks));
        });
    }

    @Test
    void shouldPersistFailedTaskWhenLlmFails() {
        Artist artist = saveArtist("Album");
        saveIndex(artist, "previous-spotify-artist");
        when(model.call(any(Prompt.class))).thenThrow(new IllegalStateException("Browser unavailable"));
        List<DiscoveryProgress> progress = new ArrayList<>();

        service.discover(saveJob(), artist, true, progress::add);

        assertThat(progress).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 3), DiscoveryProgress.Value.of(1, 3), DiscoveryProgress.Value.of(2, 3), DiscoveryProgress.Value.of(3, 3));
        assertThat(artistDataTasks()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
            assertThat(task.getResult()).contains("Browser unavailable");
            assertThat(task.getRawRequest()).isNotBlank();
            assertThat(task.getRawResult()).isEqualTo("[null]");
        });
        assertFailedTopTracks();
        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get()
                .extracting(ArtistDiscoveryIndex::getSpotifyArtistId).isEqualTo("previous-spotify-artist");
        verify(model).call(any(Prompt.class));
        assertThat(logRepository.findAll()).anySatisfy(log -> {
            assertThat(log.getLevel()).isEqualTo(LogMessage.Level.ERROR);
            assertThat(log.getText()).contains("Could not discover Spotify data", artist.getName(), artist.getId());
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCompleteTaskAndLogSkipWithoutCallingLlmForSingleNullAlbum(boolean existingIndex) {
        Artist artist = saveArtist(null);
        List<DiscoveryProgress> progress = new ArrayList<>();
        if (existingIndex) {
            saveIndex(artist, "previous-spotify-artist");
        }

        service.discover(saveJob(), artist, true, progress::add);

        assertThat(progress).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 3), DiscoveryProgress.Value.of(1, 3), DiscoveryProgress.Value.of(2, 3), DiscoveryProgress.Value.of(3, 3));
        assertThat(artistDataTasks()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(task.getResult()).isEqualTo("null");
            assertThat(task.getRawRequest()).isNull();
            assertThat(task.getRawResult()).isNull();
        });
        assertEmptyTopTracks();
        assertThat(artistDiscoveryRepository.count()).isEqualTo(1);
        assertThat(cacheRepository.count()).isZero();
        if (existingIndex) {
            assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get()
                    .extracting(ArtistDiscoveryIndex::getSpotifyArtistId).isEqualTo("previous-spotify-artist");
        } else {
            assertThat(artistDiscoveryIndexRepository.count()).isZero();
        }
        assertThat(logRepository.findAll()).anySatisfy(log -> assertThat(log.getText()).contains("no album title"));
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void shouldLoadAlbumsAndReleaseTransactionDuringSpotifyDiscovery() {
        Artist artist = saveArtist("Album");
        albumRepository.save(new Album().setArtist(artist).setName("Z album").setYear(2000));
        SpotifyArtistData result = new SpotifyArtistData(SpotifyArtistData.Status.NOT_FOUND,
                null, null, null, null, null, null, null);
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            Prompt prompt = invocation.getArgument(0);
            assertThat(prompt.getInstructions().get(1).getText())
                    .contains("Artist name: \"Artist\"", "- \"Z album\" (year: 2000)\n- \"Album\"");
            return chatResponse(result);
        });

        service.discover(saveJob(), artist, true, null);

        assertThat(artistDataTasks()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(JsonConverter.fromJson(task.getResult(), SpotifyArtistData.class)).isEqualTo(result);
        });
        assertEmptyTopTracks();
        assertThat(cacheRepository.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = SpotifyArtistData.Status.class, names = {"NOT_FOUND", "AMBIGUOUS"})
    void shouldCompleteTopTracksWithoutMatchingWhenArtistWasNotFound(SpotifyArtistData.Status status) {
        Artist artist = saveArtist("Album");
        SpotifyArtistData result = new SpotifyArtistData(status, null, null, null, null, null, null, null);
        when(model.call(any(Prompt.class))).thenReturn(chatResponse(result));

        service.discover(saveJob(), artist, true, null);

        assertEmptyTopTracks();
        ArtistDiscoveryIndex original = artistDiscoveryIndexRepository.findByArtistId(artist.getId()).orElseThrow();
        assertThat(original.getSpotifyArtistId()).isNull();
        artistDiscoveryIndexRepository.save(original.setSpotifyArtistId("previous-spotify-artist"));

        service.discover(saveJob(), artist, true, null);

        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get().satisfies(index -> {
            assertThat(index.getId()).isEqualTo(original.getId());
            assertThat(index.getSpotifyArtistId()).isNull();
        });
        assertThat(artistDiscoveryIndexRepository.count()).isOne();
        verify(model).call(any(Prompt.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCompleteTopTracksWithoutMatchingWhenTopTracksAreAbsent(boolean emptyList) {
        SpotifyArtistData found = spotifyArtistData("spotify-artist");
        SpotifyArtistData result = new SpotifyArtistData(found.status(), found.artist(), found.matchedAlbum(),
                emptyList ? List.of() : null, null, null, null, null);
        when(model.call(any(Prompt.class))).thenReturn(chatResponse(result));

        service.discover(saveJob(), saveArtist("Album"), true, null);

        assertEmptyTopTracks();
        verify(model).call(any(Prompt.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldMatchTopTracksInOrderAndPersistBothExchanges(boolean cacheEnabled) {
        Artist artist = saveArtist("Album");
        Album album = albumRepository.findAll().getFirst();
        Album otherAlbum = albumRepository.save(new Album().setArtist(artist).setName("Other album"));
        Genre genre = genreRepository.save(new Genre().setName("Genre"));
        Song first = songRepository.save(song().setPath("first").setName("First").setTrackNumber(1).setAlbum(album).setGenre(genre));
        Song second = songRepository.save(song().setPath("second").setName("Second").setTrackNumber(2).setAlbum(album).setGenre(genre));
        Song other = songRepository.save(song().setPath("other").setName("Other").setAlbum(otherAlbum).setGenre(genre));
        SpotifyArtistData found = spotifyArtistData("spotify-artist");
        SpotifyArtistData result = new SpotifyArtistData(found.status(), found.artist(), found.matchedAlbum(),
                List.of(topTrack("second", "Second", "spotify-album"), topTrack("missing", "Missing", "missing-album"),
                        topTrack("first", "First", "spotify-album")), null, null, null, null);
        String albumResponse = """
                {"albumMatches":[
                    {"spotifyAlbumId":"spotify-album","databaseAlbumId":"%s"},
                    {"spotifyAlbumId":"missing-album","databaseAlbumId":null}
                ]}
                """.formatted(album.getId());
        String trackResponse = """
                {"trackMatches":[
                    {"databaseTrackId":"%s"},
                    {"databaseTrackId":null},
                    {"databaseTrackId":"%s"}
                ]}
                """.formatted(second.getId(), first.getId());
        List<String> responses = new ArrayList<>(List.of(JsonConverter.toJson(result), albumResponse, trackResponse));
        List<Prompt> prompts = new ArrayList<>();
        List<DiscoveryProgress> progress = new ArrayList<>();
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            prompts.add(invocation.getArgument(0));
            assertThat(progress.getLast().getValue()).isEqualTo(
                    DiscoveryProgress.Value.of(prompts.size() == 1 ? 0 : 1, 4));
            if (prompts.size() > 1) {
                assertThat(artistDataTasks()).singleElement()
                        .satisfies(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE));
                assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get()
                        .extracting(ArtistDiscoveryIndex::getSpotifyArtistId).isEqualTo("spotify-artist");
                assertThat(topTracksTasks()).singleElement()
                        .satisfies(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.STARTED));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage(responses.removeFirst()))));
        });

        service.discover(saveJob(), artist, cacheEnabled, progress::add);

        assertThat(progress).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 4), DiscoveryProgress.Value.of(1, 4), DiscoveryProgress.Value.of(2, 4), DiscoveryProgress.Value.of(3, 4), DiscoveryProgress.Value.of(4, 4));
        assertThat(topTracksTasks()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(JsonConverter.fromJson(task.getResult(), String[].class)).containsExactly(second.getId(), null, first.getId());
            assertThat(JsonConverter.fromJson(task.getRawResult(), String[].class)).containsExactly(albumResponse, trackResponse);
            assertThat(JsonConverter.fromJson(task.getRawRequest(), RawRequest[].class)).hasSize(2);
            DiscoveryTask.SpotifyTopTracksParameter parameter = JsonConverter.fromJson(task.getParameter(), DiscoveryTask.SpotifyTopTracksParameter.class);
            assertThat(parameter.artistId()).isEqualTo(artist.getId());
            assertThat(parameter.spotifyArtistDataTaskId()).isEqualTo(artistDataTasks().getFirst().getId());
        });
        assertThat(prompts).hasSize(3);
        assertThat(prompts.get(1).getInstructions().get(1).getText()).contains(album.getId(), otherAlbum.getId());
        assertThat(prompts.get(2).getInstructions().get(1).getText()).contains(first.getId(), second.getId()).doesNotContain(other.getId());
        assertThat(cacheRepository.count()).isEqualTo(cacheEnabled ? 3 : 0);
    }

    @Test
    void shouldCompleteTopTracksWhenEveryTrackIsUnmatched() {
        Artist artist = saveArtist("Album");
        SpotifyArtistData found = spotifyArtistData("spotify-artist");
        SpotifyArtistData result = new SpotifyArtistData(found.status(), found.artist(), found.matchedAlbum(),
                List.of(topTrack("missing", "Missing", "missing-album")), null, null, null, null);
        when(model.call(any(Prompt.class))).thenReturn(chatResponse(result),
                new ChatResponse(List.of(new Generation(new AssistantMessage("""
                        {"albumMatches":[{"spotifyAlbumId":"missing-album","databaseAlbumId":null}]}
                        """)))),
                new ChatResponse(List.of(new Generation(new AssistantMessage("{\"trackMatches\":[{\"databaseTrackId\":null}]}")))));

        service.discover(saveJob(), artist, true, null);

        assertThat(topTracksTasks()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(task.getResult()).isEqualTo("[null]");
        });
        verify(model, times(3)).call(any(Prompt.class));
    }

    @Test
    void shouldInterruptTopTracksWhenCancelledDuringAlbumMatching() {
        Artist artist = saveArtist("Album");
        List<DiscoveryProgress> progress = new ArrayList<>();
        SpotifyArtistData found = spotifyArtistData("spotify-artist");
        SpotifyArtistData result = new SpotifyArtistData(found.status(), found.artist(), found.matchedAlbum(),
                List.of(topTrack("track", "Track", "spotify-album")), null, null, null, null);
        when(model.call(any(Prompt.class))).thenReturn(chatResponse(result)).thenAnswer(invocation -> {
            jobSynchronizer.cancelDiscovery();
            return new ChatResponse(List.of(new Generation(new AssistantMessage("{}"))));
        });

        assertThatThrownBy(() -> service.discover(saveJob(), artist, true, progress::add)).isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(progress).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, 3), DiscoveryProgress.Value.of(1, 3));
        assertThat(artistDataTasks()).singleElement()
                .satisfies(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE));
        assertThat(topTracksTasks()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.INTERRUPTED);
            assertThat(task.getResult()).isNull();
            assertThat(task.getRawResult()).isEqualTo("[null]");
        });
        assertThat(cacheRepository.count()).isEqualTo(1);
        verify(model, times(2)).call(any(Prompt.class));
    }

    @Test
    void shouldPersistInterruptedTaskAndRecoverUnfinishedTasksOnStartup() {
        Artist artist = saveArtist("Album");
        saveIndex(artist, "previous-spotify-artist");
        DiscoveryJob job = saveJob();
        when(model.call(any(Prompt.class))).thenThrow(new DiscoveryInterruptedException());

        assertThatThrownBy(() -> service.discover(job, artist, true, null)).isInstanceOf(DiscoveryInterruptedException.class);

        DiscoveryTask interruptedTask = taskRepository.findAll().getFirst();
        assertThat(interruptedTask.getStatus()).isEqualTo(DiscoveryTask.Status.INTERRUPTED);
        assertThat(artistDiscoveryIndexRepository.findByArtistId(artist.getId())).get()
                .extracting(ArtistDiscoveryIndex::getSpotifyArtistId).isEqualTo("previous-spotify-artist");
        assertThat(interruptedTask.getResult()).isNull();
        assertThat(interruptedTask.getRawRequest()).isNotBlank();
        assertThat(interruptedTask.getRawResult()).isEqualTo("[null]");
        DiscoveryTask unfinishedTask = saveTask(job, DiscoveryTask.Status.STARTED, null);
        assertThat(cacheRepository.count()).isZero();
        assertThat(logRepository.findAll()).noneMatch(log -> log.getLevel() == LogMessage.Level.ERROR);
        jobRepository.save(job.setStatus(DiscoveryJob.Status.INTERRUPTED));

        DiscoveryJob failedJob = jobRepository.save(saveJob().setStatus(DiscoveryJob.Status.FAILED));
        DiscoveryTask completedTask = saveTask(failedJob, DiscoveryTask.Status.COMPLETE, "completed result");
        DiscoveryTask failedTask = saveTask(failedJob, DiscoveryTask.Status.FAILED, "failed result");

        interruptionService.markCurrentJobsAsInterrupted();
        interruptionService.markCurrentJobsAsInterrupted();

        assertThat(taskRepository.findById(unfinishedTask.getId())).get().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.INTERRUPTED);
            assertThat(task.getResult()).isNull();
            assertThat(task.getRawRequest()).isEqualTo(unfinishedTask.getRawRequest());
            assertThat(task.getRawResult()).isNull();
        });
        assertThat(taskRepository.findById(completedTask.getId())).get().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(task.getResult()).isEqualTo("completed result");
        });
        assertThat(taskRepository.findById(failedTask.getId())).get().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
            assertThat(task.getResult()).isEqualTo("failed result");
        });
        assertThat(jobRepository.findById(job.getId())).get()
                .satisfies(value -> assertThat(value.getStatus()).isEqualTo(DiscoveryJob.Status.INTERRUPTED));
        assertThat(jobRepository.findById(failedJob.getId())).get()
                .satisfies(value -> assertThat(value.getStatus()).isEqualTo(DiscoveryJob.Status.FAILED));
    }

    @Test
    void shouldPersistInterruptionAndSkipCacheWhenCancelledDuringLlmCall() {
        Artist artist = saveArtist("Album");
        DiscoveryJob job = saveJob();
        List<DiscoveryProgress> progress = new ArrayList<>();
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            jobSynchronizer.cancelDiscovery();
            return chatResponse(spotifyArtistData("spotify-artist"));
        });

        assertThatThrownBy(() -> service.discover(job, artist, true, progress::add))
                .isInstanceOf(DiscoveryInterruptedException.class);
        assertThat(progress).extracting(DiscoveryProgress::getValue).containsExactly(DiscoveryProgress.Value.of(0, 3));
        assertThat(taskRepository.findAll()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.INTERRUPTED);
            assertThat(task.getResult()).isNull();
            assertThat(task.getRawResult()).isEqualTo("[null]");
        });
        assertThat(cacheRepository.count()).isZero();
        assertThat(logRepository.findAll()).noneMatch(log -> log.getLevel() == LogMessage.Level.ERROR);
    }

    private record RawRequest(String systemPrompt, String userPrompt, List<String> albumTitles) {}

    private List<DiscoveryTask> artistDataTasks() {
        return taskRepository.findAll().stream().filter(task -> task.getType() == DiscoveryTaskType.SPOTIFY_ARTIST_DATA).toList();
    }

    private List<DiscoveryTask> topTracksTasks() {
        return taskRepository.findAll().stream().filter(task -> task.getType() == DiscoveryTaskType.SPOTIFY_TOP_TRACKS).toList();
    }

    private void assertEmptyTopTracks() {
        assertThat(topTracksTasks()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(task.getResult()).isEqualTo("[]");
            assertThat(task.getRawRequest()).isNull();
            assertThat(task.getRawResult()).isNull();
        });
    }

    private void assertFailedTopTracks() {
        assertThat(topTracksTasks()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
            assertThat(task.getResult()).contains("SPOTIFY_ARTIST_DATA", "FAILED");
            assertThat(task.getRawRequest()).isNull();
            assertThat(task.getRawResult()).isNull();
        });
    }

    private SpotifyArtistData.TopTrack topTrack(String id, String title, String albumId) {
        return new SpotifyArtistData.TopTrack(id, title, "https://open.spotify.com/track/" + id,
                "Album", albumId, "https://open.spotify.com/album/" + albumId);
    }

    private DiscoveryTask saveTask(DiscoveryJob job, DiscoveryTask.Status status, String result) {
        return taskRepository.save(new DiscoveryTask()
                .setJob(job)
                .setType(DiscoveryTaskType.SPOTIFY_ARTIST_DATA)
                .setStatus(status)
                .setParameter("{}")
                .setResult(result));
    }

    private ChatResponse chatResponse(SpotifyArtistData result) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(JsonConverter.toJson(result)))));
    }

    private Artist saveArtist(String albumTitle) {
        Artist artist = artistRepository.save(new Artist().setName("Artist"));
        albumRepository.save(new Album().setArtist(artist).setName(albumTitle));
        return artist;
    }

    private ArtistDiscoveryIndex saveIndex(Artist artist, String spotifyArtistId) {
        return artistDiscoveryIndexRepository.save(new ArtistDiscoveryIndex()
                .setArtist(artist)
                .setSpotifyArtistId(spotifyArtistId));
    }

    private DiscoveryJob saveJob() {
        return jobRepository.save(new DiscoveryJob()
                .setType(DiscoveryType.ARTIST)
                .setStatus(DiscoveryJob.Status.STARTED));
    }

    private <R> void executeTask(ArtistDiscovery discovery, DiscoveryTaskType type, Object parameter,
                                  Function<DiscoveryTask, R> action) {
        taskExecutor.execute(new DiscoveryTaskExecution<R>() {
            @Override
            public DiscoveryTask startTask() {
                return ReflectionTestUtils.invokeMethod(service, "startTask", discovery, type, parameter);
            }

            @Override
            public R executeTask(DiscoveryTask task) {
                return action.apply(task);
            }

            @Override
            public void onCompletion(DiscoveryTask task, R result) {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                DiscoveryTask persistedTask = taskRepository.findById(task.getId()).orElseThrow();
                assertThat(persistedTask.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
                assertThat(persistedTask.getResult()).isEqualTo(JsonConverter.toJson(result));
            }

            @Override
            public void onError(DiscoveryTask task, RuntimeException error) {
                throw error;
            }
        });
    }

    private SpotifyArtistData spotifyArtistData(String spotifyArtistId) {
        return new SpotifyArtistData(SpotifyArtistData.Status.FOUND,
                new SpotifyArtistData.SpotifyArtist(spotifyArtistId, "Artist", "https://open.spotify.com/artist/" + spotifyArtistId),
                new SpotifyArtistData.MatchedAlbum("Album", "Album", "spotify-album", "https://open.spotify.com/album/spotify-album"),
                null, null, null, null, null);
    }
}
