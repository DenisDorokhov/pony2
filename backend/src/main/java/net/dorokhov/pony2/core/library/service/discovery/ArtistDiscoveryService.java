package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.discovery.spotify.SpotifyArtistDataService;
import net.dorokhov.pony2.core.library.service.discovery.spotify.SpotifyTopTracksService;
import net.dorokhov.pony2.core.library.service.image.ExternalImageStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.ARTIST_DISCOVERY;
import static net.dorokhov.pony2.core.library.service.discovery.AlbumDiscoveryService.ALBUM_TASK_COUNT;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class ArtistDiscoveryService {

    private static final Set<DiscoveryTaskType> ARTIST_TASK_TYPES = Set.of(
            DiscoveryTaskType.SPOTIFY_ARTIST_DATA,
            DiscoveryTaskType.SPOTIFY_TOP_TRACKS,
            DiscoveryTaskType.ARTIST_BIO_IMAGE,
            DiscoveryTaskType.ARTIST_AVATAR_IMAGE
    );

    public static final int ARTIST_TASK_COUNT = ARTIST_TASK_TYPES.size();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ArtistDiscoveryRepository artistDiscoveryRepository;
    private final AlbumRepository albumRepository;
    private final DiscoveryTaskRepository discoveryTaskRepository;
    private final DiscoveryTaskResultReader resultReader;
    private final SpotifyArtistDataService spotifyArtistDataService;
    private final SpotifyTopTracksService spotifyTopTracksService;
    private final AlbumDiscoveryService albumDiscoveryService;
    private final ExternalImageStorage externalImageStorage;
    private final LogService logService;
    private final DiscoveryTaskExecutor taskExecutor;
    private final LibraryJobSynchronizer jobSynchronizer;
    private final TransactionTemplate transactionTemplate;

    public ArtistDiscoveryService(
            ArtistDiscoveryRepository artistDiscoveryRepository,
            AlbumRepository albumRepository,
            DiscoveryTaskRepository discoveryTaskRepository,
            DiscoveryTaskResultReader resultReader,
            SpotifyArtistDataService spotifyArtistDataService,
            SpotifyTopTracksService spotifyTopTracksService,
            AlbumDiscoveryService albumDiscoveryService,
            ExternalImageStorage externalImageStorage,
            LogService logService,
            DiscoveryTaskExecutor taskExecutor,
            LibraryJobSynchronizer jobSynchronizer,
            PlatformTransactionManager transactionManager
    ) {
        this.artistDiscoveryRepository = artistDiscoveryRepository;
        this.albumRepository = albumRepository;
        this.discoveryTaskRepository = discoveryTaskRepository;
        this.resultReader = resultReader;
        this.spotifyArtistDataService = spotifyArtistDataService;
        this.spotifyTopTracksService = spotifyTopTracksService;
        this.albumDiscoveryService = albumDiscoveryService;
        this.externalImageStorage = externalImageStorage;
        this.logService = logService;
        this.taskExecutor = taskExecutor;
        this.jobSynchronizer = jobSynchronizer;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    public void discover(
            DiscoveryJob discoveryJob, Artist artist,
            boolean cacheEnabled, @Nullable Consumer<DiscoveryProgress> observer
    ) {
        List<Album> albums = requireNonNull(transactionTemplate.execute(status ->
                albumRepository.findByArtistId(artist.getId()).stream().sorted().toList()));
        discover(discoveryJob, artist, cacheEnabled, albums, EnumSet.copyOf(ARTIST_TASK_TYPES), album -> true, observer);
    }

    public void discoverIfUpdated(
            DiscoveryJob discoveryJob, Artist artist, List<Album> albums,
            boolean cacheEnabled, @Nullable Consumer<DiscoveryProgress> observer
    ) {
        EnumSet<DiscoveryTaskType> taskTypes = EnumSet.copyOf(ARTIST_TASK_TYPES);
        if (!shouldDiscoverArtistTask(artist, DiscoveryTaskType.SPOTIFY_ARTIST_DATA)) {
            taskTypes.remove(DiscoveryTaskType.SPOTIFY_ARTIST_DATA);
            taskTypes.removeIf(type -> type != DiscoveryTaskType.SPOTIFY_TOP_TRACKS && !shouldDiscoverArtistTask(artist, type));
        }
        discover(discoveryJob, artist, cacheEnabled, albums, taskTypes, album -> {
            jobSynchronizer.interruptDiscoveryIfCancelled();
            return shouldDiscoverAlbum(album);
        }, observer);
    }

    private boolean shouldDiscoverArtistTask(Artist artist, DiscoveryTaskType type) {
        LocalDateTime artistDate = artist.getUpdateDate() != null ? artist.getUpdateDate() : artist.getCreationDate();
        LocalDateTime spotifyDate = type != DiscoveryTaskType.SPOTIFY_ARTIST_DATA
                ? discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                artist.getId(), DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1)).orElse(artistDate)
                : artistDate;
        LocalDateTime albumDate = type == DiscoveryTaskType.SPOTIFY_TOP_TRACKS
                ? discoveryTaskRepository.findLatestCompletedMusicBrainzCreationDateByArtistId(artist.getId()).orElse(artistDate)
                : artistDate;
        LocalDateTime dependencyDate = spotifyDate.isAfter(albumDate) ? spotifyDate : albumDate;
        LocalDateTime entityDate = dependencyDate.isAfter(artistDate) ? dependencyDate : artistDate;
        return discoveryTaskRepository.findLatestCompletedCreationDateByArtistIdAndType(
                        artist.getId(), type, Limit.of(1))
                .map(taskDate -> shouldDiscoverTask(artist.getId(), type, entityDate, taskDate))
                .orElse(true);
    }

    private boolean shouldDiscoverTask(String entityId, DiscoveryTaskType type, LocalDateTime entityDate, LocalDateTime taskDate) {
        if (entityDate.isAfter(taskDate)) {
            return true;
        }
        Instant taskInstant = taskDate.atZone(ZoneId.systemDefault()).toInstant();
        return type.getImplementedAt()
                .filter(implementedAt -> implementedAt.isAfter(taskInstant))
                .map(implementedAt -> {
                    logger.debug("Re-running discovery task {} for entity '{}': implementation update date {} is after the last completed task creation date {}.",
                            type, entityId, implementedAt, taskInstant);
                    return true;
                })
                .orElse(false);
    }

    private boolean shouldDiscoverAlbum(Album album) {
        LocalDateTime entityDate = album.getUpdateDate() != null ? album.getUpdateDate() : album.getCreationDate();
        return discoveryTaskRepository.findLatestCompletedCreationDateByAlbumIdAndType(
                        album.getId(), DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, Limit.of(1))
                .map(taskDate -> shouldDiscoverTask(album.getId(), DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, entityDate, taskDate))
                .orElse(true);
    }

    private void discover(
            DiscoveryJob discoveryJob, Artist artist, boolean cacheEnabled, List<Album> albums,
            EnumSet<DiscoveryTaskType> taskTypes, Predicate<Album> shouldDiscoverAlbum,
            @Nullable Consumer<DiscoveryProgress> observer
    ) {
        long tasksComplete = ARTIST_TASK_COUNT - taskTypes.size();
        long tasksTotal = ARTIST_TASK_COUNT + (long) albums.size() * ALBUM_TASK_COUNT;
        notifyProgressObserver(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(tasksComplete, tasksTotal)), observer);
        if (taskTypes.contains(DiscoveryTaskType.SPOTIFY_ARTIST_DATA)) {
            SpotifyArtistDataTaskExecution artistDataExecution = new SpotifyArtistDataTaskExecution(discoveryJob, artist, cacheEnabled);
            taskExecutor.execute(artistDataExecution);
            notifyProgressObserver(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(++tasksComplete, tasksTotal)), observer);
        }
        for (Album album : albums) {
            if (shouldDiscoverAlbum.test(album)) {
                albumDiscoveryService.discover(discoveryJob, album, cacheEnabled, null);
            }
            updateArtistIndex(album.getArtist());
            tasksComplete += ALBUM_TASK_COUNT;
            notifyProgressObserver(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(tasksComplete, tasksTotal)), observer);
        }
        if (taskTypes.contains(DiscoveryTaskType.SPOTIFY_TOP_TRACKS)) {
            if (taskTypes.contains(DiscoveryTaskType.SPOTIFY_ARTIST_DATA) ||
                    shouldDiscoverArtistTask(artist, DiscoveryTaskType.SPOTIFY_TOP_TRACKS)) {
                taskExecutor.execute(new SpotifyTopTracksTaskExecution(discoveryJob, artist, cacheEnabled));
            }
            notifyProgressObserver(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(++tasksComplete, tasksTotal)), observer);
        }
        if (taskTypes.contains(DiscoveryTaskType.ARTIST_BIO_IMAGE)) {
            taskExecutor.execute(new ArtistImageTaskExecution(discoveryJob, artist, DiscoveryTaskType.ARTIST_BIO_IMAGE));
            notifyProgressObserver(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(++tasksComplete, tasksTotal)), observer);
        }
        if (taskTypes.contains(DiscoveryTaskType.ARTIST_AVATAR_IMAGE)) {
            taskExecutor.execute(new ArtistImageTaskExecution(discoveryJob, artist, DiscoveryTaskType.ARTIST_AVATAR_IMAGE));
            notifyProgressObserver(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(++tasksComplete, tasksTotal)), observer);
        }
    }

    private void updateArtistIndex(Artist artist) {
        transactionTemplate.executeWithoutResult(status -> {
            List<Album> albums = albumRepository.findByArtistId(artist.getId()).stream().sorted().toList();
            String musicBrainzArtistId = null;
            String countryCode = null;
            for (Album album : albums) {
                MusicBrainzAlbumData data = resultReader.readMusicBrainzAlbumData(album.getId());
                if (data == null) {
                    continue;
                }
                if (musicBrainzArtistId == null) {
                    musicBrainzArtistId = data.artistId();
                }
                if (countryCode == null && !data.country().isEmpty()) {
                    countryCode = data.country().getFirst();
                }
            }
            if (musicBrainzArtistId == null && countryCode == null) {
                return;
            }
            ArtistDiscovery index = artistDiscoveryRepository.findByArtistId(artist.getId())
                    .orElseGet(() -> new ArtistDiscovery().setArtist(artist));
            String updatedArtistId = musicBrainzArtistId != null ? musicBrainzArtistId : index.getMusicBrainzArtistId();
            String updatedCountryCode = countryCode != null ? countryCode : index.getCountryCode();
            if (!Objects.equals(index.getMusicBrainzArtistId(), updatedArtistId) ||
                    !Objects.equals(index.getCountryCode(), updatedCountryCode)) {
                artistDiscoveryRepository.save(index.setMusicBrainzArtistId(updatedArtistId).setCountryCode(updatedCountryCode));
            }
        });
    }

    private DiscoveryTask startTask(DiscoveryJob job, Artist artist, DiscoveryTaskType type) {
        return discoveryTaskRepository.save(new DiscoveryTask()
                .setJob(job)
                .setArtist(artist)
                .setType(type)
                .setStatus(DiscoveryTask.Status.STARTED));
    }

    private void notifyProgressObserver(DiscoveryProgress discoveryProgress, @Nullable Consumer<DiscoveryProgress> handler) {
        if (handler != null) {
            try {
                handler.accept(discoveryProgress);
            } catch (Throwable t) {
                logger.error("Could not call discovery progress observer.", t);
            }
        }
    }

    private class SpotifyArtistDataTaskExecution implements DiscoveryTaskExecution<SpotifyArtistData> {

        private final DiscoveryJob job;
        private final Artist artist;
        private final boolean cacheEnabled;

        private SpotifyArtistDataTaskExecution(DiscoveryJob job, Artist artist, boolean cacheEnabled) {
            this.job = job;
            this.artist = artist;
            this.cacheEnabled = cacheEnabled;
        }

        @Override
        public DiscoveryTask startTask() {
            return ArtistDiscoveryService.this.startTask(job, artist, DiscoveryTaskType.SPOTIFY_ARTIST_DATA);
        }

        @Override
        public @Nullable SpotifyArtistData executeTask(DiscoveryTask task) {
            return spotifyArtistDataService.discover(task, cacheEnabled);
        }

        @Override
        public void onResult(DiscoveryTask task, @Nullable SpotifyArtistData result) {
            ArtistDiscovery index = artistDiscoveryRepository.findByArtistId(artist.getId())
                    .orElseGet(() -> new ArtistDiscovery().setArtist(artist));
            String spotifyArtistId = result != null ? result.artist().id() : null;
            artistDiscoveryRepository.save(index.setSpotifyArtistId(spotifyArtistId));
        }

        @Override
        public void onError(DiscoveryTask task, Throwable error) {
            logService.error(logger, "Could not discover Spotify data for artist '{}'.",
                    artist.getName(), error);
        }
    }

    private class SpotifyTopTracksTaskExecution implements DiscoveryTaskExecution<List<String>> {

        private final DiscoveryJob job;
        private final Artist artist;
        private final boolean cacheEnabled;

        private SpotifyTopTracksTaskExecution(DiscoveryJob job, Artist artist, boolean cacheEnabled) {
            this.job = job;
            this.artist = artist;
            this.cacheEnabled = cacheEnabled;
        }

        @Override
        public DiscoveryTask startTask() {
            return ArtistDiscoveryService.this.startTask(job, artist, DiscoveryTaskType.SPOTIFY_TOP_TRACKS);
        }

        @Override
        public @Nullable List<String> executeTask(DiscoveryTask task) {
            return spotifyTopTracksService.discover(task, cacheEnabled);
        }

        @Override
        public void onError(DiscoveryTask task, Throwable error) {
            logService.error(logger, "Could not match Spotify top tracks for artist '{}'.",
                    artist.getName(), error);
        }
    }

    private class ArtistImageTaskExecution implements DiscoveryTaskExecution<String> {

        private final DiscoveryJob job;
        private final Artist artist;
        private final DiscoveryTaskType type;

        private ArtistImageTaskExecution(DiscoveryJob job, Artist artist, DiscoveryTaskType type) {
            this.job = job;
            this.artist = artist;
            this.type = type;
        }

        @Override
        public DiscoveryTask startTask() {
            return ArtistDiscoveryService.this.startTask(job, artist, type);
        }

        @Override
        public @Nullable String executeTask(DiscoveryTask task) {
            jobSynchronizer.interruptDiscoveryIfStopped();
            SpotifyArtistData data = transactionTemplate.execute(status -> resultReader.readSpotifyArtistData(artist.getId()));
            String url = data != null ? (type == DiscoveryTaskType.ARTIST_BIO_IMAGE ? data.imageUrl() : data.avatarUrl()) : null;
            return url != null && !url.isBlank() ? url : null;
        }

        @Override
        public void onResult(DiscoveryTask task, @Nullable String result) {
            ArtistDiscovery artistDiscovery = artistDiscoveryRepository.findByArtistId(artist.getId()).orElse(null);
            ExternalImage previousImage = artistDiscovery != null
                    ? (type == DiscoveryTaskType.ARTIST_BIO_IMAGE ? artistDiscovery.getBioExternalImage() : artistDiscovery.getAvatarExternalImage())
                    : null;
            if (previousImage != null && previousImage.getSourceUri().toString().equals(result)) {
                discoveryTaskRepository.save(task.setResult(null));
                return;
            }
            if (previousImage == null && result == null) {
                return;
            }
            ExternalImage image = null;
            if (result != null) {
                try {
                    image = externalImageStorage.downloadAndSave(URI.create(result)).getExternalImage();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                jobSynchronizer.interruptDiscoveryIfStopped();
            }
            if (artistDiscovery == null) {
                artistDiscovery = new ArtistDiscovery().setArtist(artist);
            }
            if (type == DiscoveryTaskType.ARTIST_BIO_IMAGE) {
                artistDiscovery.setBioExternalImage(image);
            } else {
                artistDiscovery.setAvatarExternalImage(image);
            }
            artistDiscoveryRepository.saveAndFlush(artistDiscovery);
            if (previousImage != null) {
                externalImageStorage.delete(previousImage.getId());
            }
            if (image != null) {
                // Replace the source URL with the saved image metadata before this transaction commits.
                discoveryTaskRepository.save(task.setResult(JsonConverter.toJson(new ArtistImageResult(image.getId(), result))));
            }
        }

        @Override
        public void onError(DiscoveryTask task, Throwable error) {
            logService.error(logger, "Could not discover image of type {} for artist '{}'.", type, artist.getName(), error);
        }
    }

    private record ArtistImageResult(String externalImageId, String sourceUri) {
    }
}
