package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.DiscoveryCancellationMonitor;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import net.dorokhov.pony2.core.llm.repository.LlmCacheRepository;
import net.dorokhov.pony2.core.log.repository.LogMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ArtistDiscoveryServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private ArtistDiscoveryService service;
    @Autowired
    private DiscoveryCancellationMonitor cancellationMonitor;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private DiscoveryJobRepository jobRepository;
    @Autowired
    private ArtistDiscoveryRepository artistDiscoveryRepository;
    @Autowired
    private DiscoveryTaskRepository taskRepository;
    @Autowired
    private DiscoveryJobInterruptionService interruptionService;
    @Autowired
    private LlmCacheRepository cacheRepository;
    @Autowired
    private LogMessageRepository logRepository;

    @BeforeEach
    void setUp() {
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
                        RawRequest rawRequest = JsonConverter.fromJson(task.getRawRequest(), RawRequest.class);
                        assertThat(rawRequest.systemPrompt()).isEqualTo(prompt.getInstructions().getFirst().getText());
                        assertThat(rawRequest.userPrompt()).isEqualTo(prompt.getInstructions().get(1).getText());
                        assertThat(rawRequest.albumTitles()).isEqualTo(List.of("Album"));
                        assertThat(task.getRawResult()).isNull();
                    });
            getTransactionTemplate().executeWithoutResult(status -> {
                ArtistDiscovery discovery = artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc(artist.getId()).orElseThrow();
                assertThat(discovery.getTasks()).singleElement()
                        .satisfies(task -> assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.STARTED));
            });
            return new ChatResponse(List.of(new Generation(new AssistantMessage(rawResponse))));
        });

        service.discover(job, artist, cacheEnabled, progress::add);
        assertThat(cacheRepository.count()).isEqualTo(cacheEnabled ? 1 : 0);
        assertThat(progress).singleElement().satisfies(value -> assertThat(value.getStep())
                .isEqualTo(DiscoveryProgress.Step.ARTIST_DISCOVERY));

        getTransactionTemplate().executeWithoutResult(status -> {
            ArtistDiscovery discovery = artistDiscoveryRepository.findFirstByArtistIdOrderByCreationDateDesc(artist.getId()).orElseThrow();
            assertThat(discovery.getTasks()).singleElement().satisfies(task -> {
                assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
                assertThat(task.getType()).isEqualTo(DiscoveryTaskType.SPOTIFY_ARTIST_DATA);
                assertThat(task.getJob().getId()).isEqualTo(job.getId());
                assertThat(task.getParameter()).isEqualTo(JsonConverter.toJson(new DiscoveryTask.ArtistParameter(artist.getId())));
                assertThat(JsonConverter.fromJson(task.getResult(), SpotifyArtistData.class)).isEqualTo(result);
                assertThat(task.getRawRequest()).isNotBlank();
                assertThat(task.getRawResult()).isEqualTo(rawResponse);
            });
        });
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
        DiscoveryTask originalTask = taskRepository.findAll().getFirst();
        clearInvocations(model);

        service.discover(saveJob(), artist, true, null);

        assertThat(taskRepository.findAll()).hasSize(2).allSatisfy(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(task.getRawRequest()).isEqualTo(originalTask.getRawRequest());
            assertThat(task.getRawResult()).isEqualTo(rawResponse);
        });
        verify(model, never()).call(any(Prompt.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not JSON", "{\"status\":\"FOUND\"}"})
    void shouldPersistRawExchangeWhenResponseIsInvalid(String rawResponse) {
        Artist artist = saveArtist("Album");
        when(model.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(rawResponse)))));

        service.discover(saveJob(), artist, true, null);

        assertThat(taskRepository.findAll()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
            assertThat(task.getRawRequest()).isNotBlank();
            assertThat(task.getRawResult()).isEqualTo(rawResponse);
            assertThat(task.getResult()).isNotBlank();
        });
        assertThat(cacheRepository.count()).isZero();
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
        when(model.call(any(Prompt.class))).thenThrow(new IllegalStateException("Browser unavailable"));

        service.discover(saveJob(), artist, true, null);

        assertThat(taskRepository.findAll()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
            assertThat(task.getResult()).contains("Browser unavailable");
            assertThat(task.getRawRequest()).isNotBlank();
            assertThat(task.getRawResult()).isNull();
        });
        assertThat(logRepository.findAll()).anySatisfy(log -> {
            assertThat(log.getLevel()).isEqualTo(LogMessage.Level.ERROR);
            assertThat(log.getText()).contains("Could not discover Spotify data", artist.getName(), artist.getId());
        });
    }

    @Test
    void shouldCompleteTaskAndLogSkipWithoutCallingLlmForSingleNullAlbum() {
        service.discover(saveJob(), saveArtist(null), true, null);

        assertThat(taskRepository.findAll()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(task.getResult()).isEqualTo("null");
            assertThat(task.getRawRequest()).isNull();
            assertThat(task.getRawResult()).isNull();
        });
        assertThat(artistDiscoveryRepository.count()).isEqualTo(1);
        assertThat(cacheRepository.count()).isZero();
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

        assertThat(taskRepository.findAll()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
            assertThat(JsonConverter.fromJson(task.getResult(), SpotifyArtistData.class)).isEqualTo(result);
        });
        assertThat(cacheRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldPersistInterruptedTaskAndRecoverUnfinishedTasksOnStartup() {
        Artist artist = saveArtist("Album");
        DiscoveryJob job = saveJob();
        when(model.call(any(Prompt.class))).thenThrow(new DiscoveryInterruptedException());

        assertThatThrownBy(() -> service.discover(job, artist, true, null)).isInstanceOf(DiscoveryInterruptedException.class);

        DiscoveryTask interruptedTask = taskRepository.findAll().getFirst();
        assertThat(interruptedTask.getStatus()).isEqualTo(DiscoveryTask.Status.INTERRUPTED);
        assertThat(interruptedTask.getResult()).isNull();
        assertThat(interruptedTask.getRawRequest()).isNotBlank();
        assertThat(interruptedTask.getRawResult()).isNull();
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
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            cancellationMonitor.cancel();
            return chatResponse(spotifyArtistData("spotify-artist"));
        });

        assertThatThrownBy(() -> service.discover(job, artist, true, null))
                .isInstanceOf(DiscoveryInterruptedException.class);
        assertThat(taskRepository.findAll()).singleElement().satisfies(task -> {
            assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.INTERRUPTED);
            assertThat(task.getResult()).isNull();
            assertThat(task.getRawResult()).isNull();
        });
        assertThat(cacheRepository.count()).isZero();
        assertThat(logRepository.findAll()).noneMatch(log -> log.getLevel() == LogMessage.Level.ERROR);
    }

    private record RawRequest(String systemPrompt, String userPrompt, List<String> albumTitles) {}

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

    private DiscoveryJob saveJob() {
        return jobRepository.save(new DiscoveryJob()
                .setType(DiscoveryType.ARTIST)
                .setStatus(DiscoveryJob.Status.STARTED));
    }

    private <P, R> void executeTask(ArtistDiscovery discovery, DiscoveryTaskType type, P parameter,
                                  Function<DiscoveryTask, R> action) {
        Consumer<RuntimeException> errorHandler = error -> {
            throw error;
        };
        ReflectionTestUtils.invokeMethod(service, "executeTask", discovery, type, parameter, action, errorHandler);
    }

    private SpotifyArtistData spotifyArtistData(String spotifyArtistId) {
        return new SpotifyArtistData(SpotifyArtistData.Status.FOUND,
                new SpotifyArtistData.SpotifyArtist(spotifyArtistId, "Artist", "https://open.spotify.com/artist/" + spotifyArtistId),
                new SpotifyArtistData.MatchedAlbum("Album", "Album", "spotify-album", "https://open.spotify.com/album/spotify-album"),
                null, null, null, null, null);
    }
}
