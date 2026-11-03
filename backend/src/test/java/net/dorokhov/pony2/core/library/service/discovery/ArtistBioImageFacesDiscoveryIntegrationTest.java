package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.discovery.spotify.SpotifyArtistDataService;
import net.dorokhov.pony2.core.library.service.discovery.spotify.SpotifyTopTracksService;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import net.dorokhov.pony2.core.library.service.image.ExternalImageStorage;
import net.dorokhov.pony2.core.library.service.image.ImageSizeReader;
import net.dorokhov.pony2.web.dto.DiscoveryTaskSummaryDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.dorokhov.pony2.api.library.domain.DiscoveryTaskType.*;
import static net.dorokhov.pony2.test.ExternalImageFixtures.saveExternalImage;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArtistBioImageFacesDiscoveryIntegrationTest extends IntegrationTest {

    @Autowired private ArtistRepository artistRepository;
    @Autowired private ArtistDiscoveryRepository artistDiscoveryRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private DiscoveryJobRepository jobRepository;
    @Autowired private DiscoveryTaskRepository taskRepository;
    @Autowired private ExternalImageRepository imageRepository;
    @Autowired private ExternalImageStorage imageStorage;
    @Autowired private ImageSizeReader imageSizeReader;
    @Autowired private DiscoveryTaskResultReader resultReader;
    @Autowired private DiscoveryTaskLlmExecutor llmExecutor;
    @Autowired private DiscoveryTaskExecutor taskExecutor;
    @Autowired private AlbumDiscoveryService albumDiscoveryService;
    @Autowired private LibraryJobSynchronizer jobSynchronizer;
    @Autowired private LogService logService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Value("${pony.externalImage.path}") private File imageDirectory;

    private final SpotifyArtistDataService spotify = mock(SpotifyArtistDataService.class);
    private final SpotifyTopTracksService topTracks = mock(SpotifyTopTracksService.class);
    private LibraryJobSynchronizer.LibraryJobRegistration registration;
    private Artist artist;
    private ExternalImageFile image;
    private ArtistDiscoveryService service;

    @BeforeEach
    void setUp() throws Exception {
        registration = jobSynchronizer.registerDiscoveryJob();
        artist = artistRepository.save(new Artist().setName("Artist"));
        image = saveExternalImage(imageRepository, imageDirectory, "image.png");
        artistDiscoveryRepository.save(new ArtistDiscovery().setArtist(artist).setBioExternalImage(image.getExternalImage()));
        ConfigService configuration = mock(ConfigService.class);
        when(configuration.get()).thenReturn(new ConfigSet(null, List.of(), true, "http://localhost/v1", "vision-model", null));
        ArtistBioImageFacesService faces = new ArtistBioImageFacesService(llmExecutor, artistDiscoveryRepository, taskRepository,
                imageStorage, imageSizeReader, configuration, transactionManager, new ClassPathResource("prompts/artist-bio-image-faces.txt"));
        service = new ArtistDiscoveryService(artistDiscoveryRepository, albumRepository, taskRepository, resultReader,
                spotify, topTracks, albumDiscoveryService, imageStorage, faces, logService, taskExecutor, jobSynchronizer, transactionManager);
        when(spotify.discover(any(), anyBoolean())).thenReturn(spotifyData());
        when(model.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return response("{\"faces\": [[100,200,300,500]]}");
        });
    }

    private SpotifyArtistData spotifyData() {
        return new SpotifyArtistData(new SpotifyArtistData.SpotifyArtist("spotify-id", "Artist", "https://open.spotify.com/artist/spotify-id"),
                new SpotifyArtistData.MatchedAlbum("Album", "Album", "album-id", "https://open.spotify.com/album/album-id"),
                null, null, null, null, image.getExternalImage().getSourceUri().toString(), null);
    }

    private ChatResponse response(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    @AfterEach
    void tearDown() throws ConcurrentLibraryJobException {
        registration.close();
        if (jobSynchronizer.isDiscoveryCancelled()) {
            jobSynchronizer.registerDiscoveryJob().close();
        }
    }

    @Test
    void shouldRunFacesLastAndPersistOnlyTaskResultAndProgress() {
        List<DiscoveryProgress> progress = new ArrayList<>();

        service.discover(job(), artist, false, progress::add);

        assertThat(taskRepository.findAll().stream().sorted(Comparator.comparing(DiscoveryTask::getCreationDate)))
                .extracting(DiscoveryTask::getType).containsExactly(SPOTIFY_ARTIST_DATA, SPOTIFY_TOP_TRACKS,
                        ARTIST_AVATAR_IMAGE, ARTIST_BIO_IMAGE, ARTIST_BIO_IMAGE_FACES);
        assertThat(progress).extracting(value -> value.getValue().getItemsComplete()).containsExactly(0L, 1L, 2L, 3L, 4L, 5L);
        DiscoveryTask task = facesTask();
        assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        ArtistBioImageFacesService.Result result = JsonConverter.fromJson(task.getResult(), ArtistBioImageFacesService.Result.class);
        assertThat(result.externalImageId()).isEqualTo(image.getExternalImage().getId());
        assertThat(result.faces()).containsExactly(new ImageFace(0.1, 0.2, 0.3, 0.5));
        assertThat(resultReader.readArtistBioImageFaces(artist.getId(), result.externalImageId())).isEqualTo(result.faces());
        assertThat(resultReader.readArtistBioImageFaces(artist.getId(), "another-image")).isNull();
        assertThat(task.getRawRequests()).hasSize(1);
        assertThat(task.getRawResponses()).hasSize(1);
        assertThat(imageRepository.count()).isOne();
    }

    private DiscoveryJob job() {
        return jobRepository.save(new DiscoveryJob().setType(DiscoveryType.ARTIST).setStatus(DiscoveryJob.Status.STARTED));
    }

    private DiscoveryTask facesTask() {
        return taskRepository.findAll().stream().filter(task -> task.getType() == ARTIST_BIO_IMAGE_FACES)
                .max(Comparator.comparing(DiscoveryTask::getCreationDate)).orElseThrow();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"faces\": []}", "{\"faces\": [[0,0,1000,1000]]}"})
    void shouldSkipUnchangedSuccessfulAnalysisIncludingNoFaces(String content) {
        when(model.call(any(Prompt.class))).thenReturn(response(content));
        service.discover(job(), artist, true, null);
        DiscoveryTask completed = facesTask();
        clearInvocations(model);

        DiscoveryJob next = job();
        service.discoverIfUpdated(next, artist, List.of(), true, null);

        assertThat(completed.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(completed.getResult()).isNotNull();
        assertThat(taskRepository.countByJobId(next.getId())).isZero();
        verifyNoInteractions(model);
        if (content.contains("[]")) {
            List<ImageFace> faces = resultReader.readArtistBioImageFaces(artist.getId(), image.getExternalImage().getId());
            assertThat(faces).isEmpty();
            assertThat(completed.getResult()).contains("\"faces\":[]");
        }
    }

    @Test
    void shouldBackfillMissingFacesTaskWithoutRepeatingOtherArtistTasks() {
        service.discover(job(), artist, false, null);
        taskRepository.deleteById(facesTask().getId());
        clearInvocations(model, spotify);

        DiscoveryJob next = job();
        service.discoverIfUpdated(next, artist, List.of(), false, null);

        assertThat(taskRepository.findAll()).filteredOn(task -> task.getJob().getId().equals(next.getId()))
                .singleElement().satisfies(task -> assertThat(task.getType()).isEqualTo(ARTIST_BIO_IMAGE_FACES));
        verify(model).call(any(Prompt.class));
        verifyNoInteractions(spotify);
    }

    @Test
    void shouldAnalyzeReplacementAndNeverExposeOldImageFaces() throws Exception {
        service.discover(job(), artist, false, null);
        ExternalImageFile replacement = saveExternalImage(imageRepository, imageDirectory, "red.png");
        getTransactionTemplate().executeWithoutResult(status -> {
            ArtistDiscovery discovery = artistDiscoveryRepository.findByArtistId(artist.getId()).orElseThrow();
            discovery.setBioExternalImage(replacement.getExternalImage());
        });
        assertThat(resultReader.readArtistBioImageFaces(artist.getId(), replacement.getExternalImage().getId())).isNull();
        clearInvocations(model);

        service.discoverIfUpdated(job(), artist, List.of(), false, null);

        assertThat(facesTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(JsonConverter.fromJson(facesTask().getResult(), ArtistBioImageFacesService.Result.class).externalImageId())
                .isEqualTo(replacement.getExternalImage().getId());
        assertThat(resultReader.readArtistBioImageFaces(artist.getId(), replacement.getExternalImage().getId())).hasSize(1);
        verify(model).call(any(Prompt.class));
    }

    @Test
    void shouldRetryFailedAnalysisWithoutTreatingItAsNoFaces() {
        when(model.call(any(Prompt.class))).thenReturn(response("{\"faces\": [[100,200,1001,500]]}"));
        service.discover(job(), artist, true, null);
        assertThat(facesTask().getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
        assertThat(resultReader.readArtistBioImageFaces(artist.getId(), image.getExternalImage().getId())).isNull();
        when(model.call(any(Prompt.class))).thenReturn(response("{\"faces\": []}"));

        service.discoverIfUpdated(job(), artist, List.of(), true, null);

        assertThat(facesTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(resultReader.readArtistBioImageFaces(artist.getId(), image.getExternalImage().getId())).isEmpty();
    }

    @Test
    void shouldKeepPreviousAcceptedFacesAfterFailedForcedAnalysis() {
        service.discover(job(), artist, false, null);
        when(model.call(any(Prompt.class))).thenReturn(response("{\"faces\": [[500,200,100,500]]}"));

        service.discover(job(), artist, false, null);

        assertThat(facesTask().getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
        assertThat(resultReader.readArtistBioImageFaces(artist.getId(), image.getExternalImage().getId()))
                .containsExactly(new ImageFace(0.1, 0.2, 0.3, 0.5));
    }

    @Test
    void shouldCompleteWithNoResultWithoutModelWhenBiographyImageIsAbsent() {
        removeBiographyImage();

        service.discover(job(), artist, true, null);

        DiscoveryTask task = facesTask();
        assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(task.getResult()).isNull();
        assertThat(DiscoveryTaskSummaryDto.of(task, null, null, null, null).isNoResult()).isTrue();

        DiscoveryJob next = job();
        service.discoverIfUpdated(next, artist, List.of(), true, null);

        assertThat(taskRepository.countByJobId(next.getId())).isZero();
        verify(model, never()).call(any(Prompt.class));
    }

    private void removeBiographyImage() {
        getTransactionTemplate().executeWithoutResult(status -> {
            ArtistDiscovery discovery = artistDiscoveryRepository.findByArtistId(artist.getId()).orElseThrow();
            discovery.setBioExternalImage(null);
        });
        when(spotify.discover(any(), anyBoolean())).thenReturn(new SpotifyArtistData(spotifyData().artist(), spotifyData().matchedAlbum(),
                null, null, null, null, null, null));
    }

    @Test
    void shouldReplaceLegacyMissingImageResultWithNoResultOnlyOnce() {
        removeBiographyImage();
        service.discover(job(), artist, true, null);
        DiscoveryTask task = facesTask();
        taskRepository.save(task.setResult(JsonConverter.toJson(new ArtistBioImageFacesService.Result(null, null, List.of()))));

        DiscoveryJob backfill = job();
        service.discoverIfUpdated(backfill, artist, List.of(), true, null);

        assertThat(taskRepository.findAll()).filteredOn(candidate -> candidate.getJob().getId().equals(backfill.getId()))
                .singleElement().satisfies(candidate -> {
                    assertThat(candidate.getType()).isEqualTo(ARTIST_BIO_IMAGE_FACES);
                    assertThat(candidate.getResult()).isNull();
                    assertThat(DiscoveryTaskSummaryDto.of(candidate, null, null, null, null).isNoResult()).isTrue();
                });
        DiscoveryJob following = job();
        service.discoverIfUpdated(following, artist, List.of(), true, null);
        assertThat(taskRepository.countByJobId(following.getId())).isZero();
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void shouldInterruptWithoutPublishingFacesWhenDiscoveryIsCancelled() {
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            jobSynchronizer.cancelDiscovery();
            return response("{\"faces\": []}");
        });

        assertThatThrownBy(() -> service.discover(job(), artist, false, null)).isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(facesTask().getStatus()).isEqualTo(DiscoveryTask.Status.INTERRUPTED);
        assertThat(facesTask().getResult()).isNull();
        assertThat(resultReader.readArtistBioImageFaces(artist.getId(), image.getExternalImage().getId())).isNull();
    }
}
