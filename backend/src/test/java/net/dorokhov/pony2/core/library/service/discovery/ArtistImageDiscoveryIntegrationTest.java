package net.dorokhov.pony2.core.library.service.discovery;

import com.sun.net.httpserver.HttpServer;
import jakarta.annotation.Nullable;
import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscovery;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;
import net.dorokhov.pony2.api.library.domain.ExternalImage;
import net.dorokhov.pony2.api.library.domain.ExternalImageFile;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryJobRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.repository.ExternalImageRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.discovery.spotify.SpotifyArtistDataService;
import net.dorokhov.pony2.core.library.service.discovery.spotify.SpotifyTopTracksService;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import net.dorokhov.pony2.core.library.service.file.FileTypeResolver;
import net.dorokhov.pony2.core.library.service.image.ExternalImageStorage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.api.library.domain.DiscoveryTaskType.ARTIST_AVATAR_IMAGE;
import static net.dorokhov.pony2.api.library.domain.DiscoveryTaskType.ARTIST_BIO_IMAGE;
import static net.dorokhov.pony2.core.library.service.discovery.ArtistDiscoveryService.ARTIST_TASK_COUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArtistImageDiscoveryIntegrationTest extends IntegrationTest {

    @Autowired private ArtistRepository artistRepository;
    @Autowired private ArtistDiscoveryRepository artistDiscoveryRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private DiscoveryJobRepository jobRepository;
    @Autowired private DiscoveryTaskRepository taskRepository;
    @Autowired private ExternalImageRepository externalImageRepository;
    @Autowired private ExternalImageStorage externalImageStorage;
    @Autowired private LibraryJobSynchronizer jobSynchronizer;
    @Autowired private DiscoveryTaskResultReader resultReader;
    @Autowired private SpotifyTopTracksService spotifyTopTracksService;
    @Autowired private AlbumDiscoveryService albumDiscoveryService;
    @Autowired private LogService logService;
    @Autowired private DiscoveryTaskExecutor taskExecutor;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private FileTypeResolver fileTypeResolver;
    @Value("${pony.externalImage.path}") private File externalImageFolder;

    private static final HttpServer proxyServer = createProxyServer();
    private final SpotifyArtistDataService spotifyArtistDataService = mock(SpotifyArtistDataService.class);
    private final ArtistBioImageFacesService artistBioImageFacesService = mock(ArtistBioImageFacesService.class);
    private final AtomicInteger downloads = new AtomicInteger();
    private final AtomicBoolean cancelDownload = new AtomicBoolean();
    private final AtomicBoolean missingImageAvailable = new AtomicBoolean();
    private final List<DiscoveryTaskType> downloadingTasks = new CopyOnWriteArrayList<>();
    private LibraryJobSynchronizer.LibraryJobRegistration jobRegistration;
    private Artist artist;
    private byte[] content;
    private ArtistDiscoveryService service;
    private ArtistDiscoveryRepository taskArtistDiscoveryRepository;

    private static HttpServer createProxyServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        jobRegistration = jobSynchronizer.registerDiscoveryJob();
        artist = artistRepository.save(new Artist().setName("Artist"));
        content = Files.readAllBytes(new ClassPathResource("image.png").getFile().toPath());
        proxyServer.createContext("/", exchange -> {
            downloads.incrementAndGet();
            downloadingTasks.addAll(taskRepository.findAll().stream()
                    .filter(task -> task.getStatus() == DiscoveryTask.Status.STARTED)
                    .map(DiscoveryTask::getType).toList());
            if (cancelDownload.get()) {
                jobSynchronizer.cancelDiscovery();
            }
            String path = exchange.getRequestURI().getPath();
            byte[] body = path.equals("/invalid") ? "Not an image".getBytes(UTF_8) : content;
            try (exchange) {
                boolean missingImage = path.equals("/missing") && !missingImageAvailable.get();
                exchange.sendResponseHeaders(missingImage ? 404 : 200, body.length);
                exchange.getResponseBody().write(body);
            }
        });
        taskArtistDiscoveryRepository = mock(ArtistDiscoveryRepository.class, delegatesTo(artistDiscoveryRepository));
        ExternalImageStorage imageStorage = new ExternalImageStorage(externalImageRepository, fileTypeResolver,
                externalImageFolder, URI.create("http://127.0.0.1:" + proxyServer.getAddress().getPort()));
        service = new ArtistDiscoveryService(taskArtistDiscoveryRepository, albumRepository, taskRepository, resultReader,
                spotifyArtistDataService, spotifyTopTracksService, albumDiscoveryService, imageStorage, artistBioImageFacesService, logService,
                taskExecutor, jobSynchronizer, transactionManager);
        when(spotifyArtistDataService.discover(any(), anyBoolean())).thenReturn(artistData("/bio", "/avatar"));
    }

    private SpotifyArtistData artistData(@Nullable String bioPath, @Nullable String avatarPath) {
        return new SpotifyArtistData(
                new SpotifyArtistData.SpotifyArtist("spotify-artist", "Artist", "https://open.spotify.com/artist/spotify-artist"),
                new SpotifyArtistData.MatchedAlbum("Album", "Album", "spotify-album", "https://open.spotify.com/album/spotify-album"),
                null, null, null, null, bioPath != null ? sourceUri(bioPath).toString() : null,
                avatarPath != null ? sourceUri(avatarPath).toString() : null);
    }

    private URI sourceUri(String path) {
        return URI.create("http://images.invalid" + path);
    }

    @AfterEach
    void tearDown() {
        proxyServer.removeContext("/");
        jobRegistration.close();
    }

    @AfterAll
    static void stopProxyServer() {
        proxyServer.stop(0);
    }

    @Test
    void shouldDownloadImagesLastAndPersistResultsAndProgress() throws IOException {
        List<DiscoveryProgress> progress = new ArrayList<>();

        discover(progress::add);

        assertThat(taskRepository.findAll().stream().sorted(Comparator.comparing(DiscoveryTask::getCreationDate)))
                .extracting(DiscoveryTask::getType).containsExactly(DiscoveryTaskType.SPOTIFY_ARTIST_DATA,
                        DiscoveryTaskType.SPOTIFY_TOP_TRACKS, ARTIST_AVATAR_IMAGE, ARTIST_BIO_IMAGE, DiscoveryTaskType.ARTIST_BIO_IMAGE_FACES);
        assertThat(downloadingTasks).containsExactly(ARTIST_AVATAR_IMAGE, ARTIST_BIO_IMAGE);
        assertThat(progress).extracting(DiscoveryProgress::getValue).containsExactly(
                DiscoveryProgress.Value.of(0, ARTIST_TASK_COUNT), DiscoveryProgress.Value.of(1, ARTIST_TASK_COUNT), DiscoveryProgress.Value.of(2, ARTIST_TASK_COUNT),
                DiscoveryProgress.Value.of(3, ARTIST_TASK_COUNT), DiscoveryProgress.Value.of(4, ARTIST_TASK_COUNT),
                DiscoveryProgress.Value.of(ARTIST_TASK_COUNT, ARTIST_TASK_COUNT));
        for (DiscoveryTaskType type : List.of(ARTIST_BIO_IMAGE, ARTIST_AVATAR_IMAGE)) {
            ExternalImageFile image = image(type);
            assertThat(Files.readAllBytes(image.getFile().toPath())).isEqualTo(content);
            assertImageResult(type, image);
        }
        assertThat(externalImageRepository.count()).isEqualTo(2);
    }

    private void discover(@Nullable Consumer<DiscoveryProgress> observer) {
        DiscoveryJob job = jobRepository.save(new DiscoveryJob().setType(DiscoveryType.ARTIST).setStatus(DiscoveryJob.Status.STARTED));
        service.discover(job, artist, true, observer);
    }

    private ExternalImageFile image(DiscoveryTaskType type) {
        return requireNonNull(getTransactionTemplate().execute(status -> {
            ArtistDiscovery index = artistDiscoveryRepository.findByArtistId(artist.getId()).orElseThrow();
            ExternalImage image = type == ARTIST_BIO_IMAGE ? index.getBioExternalImage() : index.getAvatarExternalImage();
            return externalImageStorage.getImageFile(requireNonNull(image).getId()).orElseThrow();
        }));
    }

    private void assertImageResult(DiscoveryTaskType type, ExternalImageFile image) {
        DiscoveryTask task = latestTask(type);
        assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(JsonConverter.mapFromJson(task.getResult(), String.class, String.class)).isEqualTo(Map.of(
                "externalImageId", image.getExternalImage().getId(), "sourceUri", image.getExternalImage().getSourceUri().toString()));
    }

    private DiscoveryTask latestTask(DiscoveryTaskType type) {
        return taskRepository.findAll().stream().filter(task -> task.getType() == type)
                .max(Comparator.comparing(DiscoveryTask::getCreationDate)).orElseThrow();
    }

    @Test
    void shouldKeepImagesWithoutDownloadingWhenUrlsHaveNotChanged() {
        discover(null);
        ExternalImageFile bio = image(ARTIST_BIO_IMAGE);
        ExternalImageFile avatar = image(ARTIST_AVATAR_IMAGE);

        discover(null);

        assertThat(image(ARTIST_BIO_IMAGE).getExternalImage().getId()).isEqualTo(bio.getExternalImage().getId());
        assertThat(image(ARTIST_AVATAR_IMAGE).getExternalImage().getId()).isEqualTo(avatar.getExternalImage().getId());
        assertThat(bio.getFile()).exists();
        assertThat(avatar.getFile()).exists();
        assertThat(downloads.get()).isEqualTo(2);
        assertThat(externalImageRepository.count()).isEqualTo(2);
        assertImageResult(ARTIST_BIO_IMAGE, bio);
        assertImageResult(ARTIST_AVATAR_IMAGE, avatar);
    }

    private void assertNoImageResult(DiscoveryTaskType type) {
        DiscoveryTask task = latestTask(type);
        assertThat(task.getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(task.getResult()).isNull();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldClearImagesAndDeleteTheirFilesWhenArtistDataOrUrlsAreAbsent(boolean missingData) {
        discover(null);
        ExternalImageFile bio = image(ARTIST_BIO_IMAGE);
        ExternalImageFile avatar = image(ARTIST_AVATAR_IMAGE);
        SpotifyArtistData data = missingData ? null : artistData(null, null);
        when(spotifyArtistDataService.discover(any(), anyBoolean())).thenReturn(data);

        discover(null);

        getTransactionTemplate().executeWithoutResult(status -> {
            ArtistDiscovery index = artistDiscoveryRepository.findByArtistId(artist.getId()).orElseThrow();
            assertThat(index.getBioExternalImage()).isNull();
            assertThat(index.getAvatarExternalImage()).isNull();
        });
        assertThat(bio.getFile()).doesNotExist();
        assertThat(avatar.getFile()).doesNotExist();
        assertThat(externalImageRepository.count()).isZero();
        assertThat(downloads.get()).isEqualTo(2);
        assertNoImageResult(ARTIST_BIO_IMAGE);
        assertNoImageResult(ARTIST_AVATAR_IMAGE);
    }

    @ParameterizedTest
    @EnumSource(value = DiscoveryTaskType.class, names = {"ARTIST_BIO_IMAGE", "ARTIST_AVATAR_IMAGE"})
    void shouldReplaceOnlyTheChangedImageAndDeleteItsPreviousFile(DiscoveryTaskType type) {
        discover(null);
        ExternalImageFile previous = image(type);
        DiscoveryTaskType otherType = type == ARTIST_BIO_IMAGE ? ARTIST_AVATAR_IMAGE : ARTIST_BIO_IMAGE;
        ExternalImageFile other = image(otherType);
        when(spotifyArtistDataService.discover(any(), anyBoolean())).thenReturn(artistData(
                type == ARTIST_BIO_IMAGE ? "/replacement" : "/bio", type == ARTIST_AVATAR_IMAGE ? "/replacement" : "/avatar"));

        discover(null);

        ExternalImageFile replacement = image(type);
        assertThat(replacement.getExternalImage().getId()).isNotEqualTo(previous.getExternalImage().getId());
        assertThat(replacement.getExternalImage().getSourceUri()).isEqualTo(sourceUri("/replacement"));
        assertImageResult(type, replacement);
        assertThat(previous.getFile()).doesNotExist();
        assertThat(externalImageRepository.existsById(previous.getExternalImage().getId())).isFalse();
        assertThat(image(otherType).getExternalImage().getId()).isEqualTo(other.getExternalImage().getId());
        assertThat(other.getFile()).exists();
        assertImageResult(otherType, other);
        assertThat(downloads.get()).isEqualTo(3);
        assertThat(externalImageRepository.count()).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({"ARTIST_BIO_IMAGE,/missing,404", "ARTIST_BIO_IMAGE,/invalid,not an image",
            "ARTIST_AVATAR_IMAGE,/missing,404", "ARTIST_AVATAR_IMAGE,/invalid,not an image"})
    void shouldKeepPreviousImageAfterDownloadFailureAndCompleteTheOtherTask(DiscoveryTaskType type, String path, String error) {
        discover(null);
        ExternalImageFile previous = image(type);
        DiscoveryTaskType otherType = type == ARTIST_BIO_IMAGE ? ARTIST_AVATAR_IMAGE : ARTIST_BIO_IMAGE;
        ExternalImageFile other = image(otherType);
        when(spotifyArtistDataService.discover(any(), anyBoolean())).thenReturn(artistData(
                type == ARTIST_BIO_IMAGE ? path : "/replacement", type == ARTIST_AVATAR_IMAGE ? path : "/replacement"));

        discover(null);

        assertThat(latestTask(type).getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
        assertThat(latestTask(type).getResult()).contains(error);
        assertThat(image(type).getExternalImage().getId()).isEqualTo(previous.getExternalImage().getId());
        assertThat(previous.getFile()).exists();
        assertImageResult(otherType, image(otherType));
        assertThat(other.getFile()).doesNotExist();
        assertThat(externalImageRepository.count()).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(value = DiscoveryTaskType.class, names = {"ARTIST_BIO_IMAGE", "ARTIST_AVATAR_IMAGE"})
    void shouldRetryFailedImageReplacementDuringFullDiscoveryWithoutRefreshingSpotifyData(DiscoveryTaskType type) {
        discover(null);
        ExternalImageFile previous = image(type);
        DiscoveryTaskType otherType = type == ARTIST_BIO_IMAGE ? ARTIST_AVATAR_IMAGE : ARTIST_BIO_IMAGE;
        ExternalImageFile other = image(otherType);
        when(spotifyArtistDataService.discover(any(), anyBoolean())).thenReturn(artistData(
                type == ARTIST_BIO_IMAGE ? "/missing" : "/bio", type == ARTIST_AVATAR_IMAGE ? "/missing" : "/avatar"));
        discover(null);
        assertThat(latestTask(type).getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
        missingImageAvailable.set(true);

        DiscoveryJob retryJob = discoverAll();

        assertThat(taskRepository.findAll()).filteredOn(task -> task.getJob().getId().equals(retryJob.getId()))
                .singleElement().satisfies(task -> assertThat(task.getType()).isEqualTo(type));
        ExternalImageFile replacement = image(type);
        assertThat(replacement.getExternalImage().getSourceUri()).isEqualTo(sourceUri("/missing"));
        assertImageResult(type, replacement);
        assertThat(previous.getFile()).doesNotExist();
        assertThat(image(otherType).getExternalImage().getId()).isEqualTo(other.getExternalImage().getId());
        assertThat(other.getFile()).exists();
        assertThat(downloads.get()).isEqualTo(4);
        assertThat(externalImageRepository.count()).isEqualTo(2);

        DiscoveryJob followingJob = discoverAll();

        assertThat(taskRepository.countByJobId(followingJob.getId())).isZero();
    }

    private DiscoveryJob discoverAll() {
        DiscoveryJob job = jobRepository.save(new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.STARTED));
        FullDiscoveryService fullDiscoveryService = new FullDiscoveryService(artistRepository, albumRepository, service, jobSynchronizer);
        fullDiscoveryService.discover(job, true, null);
        return job;
    }

    @Test
    void shouldFailBothImageTasksWhenNoCompletedSpotifyArtistDataExists() {
        when(spotifyArtistDataService.discover(any(), anyBoolean())).thenThrow(new IllegalStateException("Spotify unavailable"));

        discover(null);

        for (DiscoveryTaskType type : List.of(ARTIST_BIO_IMAGE, ARTIST_AVATAR_IMAGE)) {
            assertThat(latestTask(type).getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
            assertThat(latestTask(type).getResult()).contains("No completed SPOTIFY_ARTIST_DATA task");
        }
        assertThat(downloads.get()).isZero();
        assertThat(externalImageRepository.count()).isZero();
    }

    @Test
    void shouldRollBackNewImageAndKeepPreviousFileWhenIndexSavingFails() throws IOException {
        discover(null);
        ExternalImageFile previous = image(ARTIST_BIO_IMAGE);
        when(spotifyArtistDataService.discover(any(), anyBoolean())).thenReturn(artistData("/replacement", "/avatar"));
        doThrow(new IllegalStateException("Could not save image index")).when(taskArtistDiscoveryRepository).saveAndFlush(any());

        discover(null);

        assertThat(latestTask(ARTIST_BIO_IMAGE).getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
        assertThat(latestTask(ARTIST_BIO_IMAGE).getResult()).contains("Could not save image index");
        assertThat(image(ARTIST_BIO_IMAGE).getExternalImage().getId()).isEqualTo(previous.getExternalImage().getId());
        assertThat(previous.getFile()).exists();
        assertImageResult(ARTIST_AVATAR_IMAGE, image(ARTIST_AVATAR_IMAGE));
        assertThat(externalImageRepository.count()).isEqualTo(2);
        assertImageFileCount(2);
    }

    private void assertImageFileCount(long count) throws IOException {
        try (Stream<Path> files = Files.walk(externalImageFolder.toPath())) {
            assertThat(files.filter(Files::isRegularFile).count()).isEqualTo(count);
        }
    }

    @Test
    void shouldRollBackDownloadedImageWhenDiscoveryIsCancelled() throws IOException {
        discover(null);
        ExternalImageFile previous = image(ARTIST_BIO_IMAGE);
        when(spotifyArtistDataService.discover(any(), anyBoolean())).thenReturn(artistData("/replacement", "/avatar"));
        cancelDownload.set(true);

        assertThatThrownBy(() -> discover(null)).isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(latestTask(ARTIST_BIO_IMAGE).getStatus()).isEqualTo(DiscoveryTask.Status.INTERRUPTED);
        assertThat(latestTask(ARTIST_BIO_IMAGE).getResult()).isNull();
        assertThat(image(ARTIST_BIO_IMAGE).getExternalImage().getId()).isEqualTo(previous.getExternalImage().getId());
        assertThat(previous.getFile()).exists();
        assertThat(externalImageRepository.count()).isEqualTo(2);
        assertImageFileCount(2);
    }

}
