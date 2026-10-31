package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscoveryIndex;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.MusicBrainzAlbumData;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryIndexRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.discovery.spotify.SpotifyArtistDataService;
import net.dorokhov.pony2.core.library.service.discovery.spotify.SpotifyTopTracksService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.ARTIST_DISCOVERY;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class ArtistDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ArtistDiscoveryIndexRepository artistDiscoveryIndexRepository;
    private final AlbumRepository albumRepository;
    private final DiscoveryTaskRepository discoveryTaskRepository;
    private final DiscoveryTaskResultReader resultReader;
    private final SpotifyArtistDataService spotifyArtistDataService;
    private final SpotifyTopTracksService spotifyTopTracksService;
    private final AlbumDiscoveryService albumDiscoveryService;
    private final LogService logService;
    private final DiscoveryTaskExecutor taskExecutor;
    private final LibraryJobSynchronizer jobSynchronizer;
    private final TransactionTemplate transactionTemplate;

    public ArtistDiscoveryService(
            ArtistDiscoveryIndexRepository artistDiscoveryIndexRepository,
            AlbumRepository albumRepository,
            DiscoveryTaskRepository discoveryTaskRepository,
            DiscoveryTaskResultReader resultReader,
            SpotifyArtistDataService spotifyArtistDataService,
            SpotifyTopTracksService spotifyTopTracksService,
            AlbumDiscoveryService albumDiscoveryService,
            LogService logService,
            DiscoveryTaskExecutor taskExecutor,
            LibraryJobSynchronizer jobSynchronizer,
            PlatformTransactionManager transactionManager
    ) {
        this.artistDiscoveryIndexRepository = artistDiscoveryIndexRepository;
        this.albumRepository = albumRepository;
        this.discoveryTaskRepository = discoveryTaskRepository;
        this.resultReader = resultReader;
        this.spotifyArtistDataService = spotifyArtistDataService;
        this.spotifyTopTracksService = spotifyTopTracksService;
        this.albumDiscoveryService = albumDiscoveryService;
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
        discover(discoveryJob, artist, cacheEnabled, albums, true, true, album -> true, observer);
    }

    public void discoverIfUpdated(
            DiscoveryJob discoveryJob, Artist artist, List<Album> albums,
            boolean cacheEnabled, @Nullable Consumer<DiscoveryProgress> observer
    ) {
        boolean discoverArtist = shouldDiscoverArtistTask(artist, DiscoveryTaskType.SPOTIFY_ARTIST_DATA);
        boolean discoverTopTracks = discoverArtist || shouldDiscoverArtistTask(artist, DiscoveryTaskType.SPOTIFY_TOP_TRACKS);
        discover(discoveryJob, artist, cacheEnabled, albums, discoverArtist, discoverTopTracks, album -> {
            jobSynchronizer.interruptDiscoveryIfCancelled();
            return shouldDiscoverAlbum(album);
        }, observer);
    }

    private boolean shouldDiscoverArtistTask(Artist artist, DiscoveryTaskType type) {
        LocalDateTime entityDate = artist.getUpdateDate() != null ? artist.getUpdateDate() : artist.getCreationDate();
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

    private void discover(DiscoveryJob discoveryJob, Artist artist, boolean cacheEnabled, List<Album> albums,
                          boolean discoverArtist, boolean discoverTopTracks, Predicate<Album> shouldDiscoverAlbum,
                          @Nullable Consumer<DiscoveryProgress> observer) {
        long tasksComplete = (discoverArtist ? 0 : 1) + (discoverTopTracks ? 0 : 1);
        long tasksTotal = 2 + albums.size();
        notifyProgressObserver(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(tasksComplete, tasksTotal)), observer);
        if (discoverArtist) {
            SpotifyArtistDataTaskExecution artistDataExecution = new SpotifyArtistDataTaskExecution(discoveryJob, artist, cacheEnabled);
            taskExecutor.execute(artistDataExecution);
            notifyProgressObserver(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(++tasksComplete, tasksTotal)), observer);
        }
        for (Album album : albums) {
            if (shouldDiscoverAlbum.test(album)) {
                albumDiscoveryService.discover(discoveryJob, album, cacheEnabled, null);
            }
            updateArtistIndex(album.getArtist());
            notifyProgressObserver(new DiscoveryProgress(ARTIST_DISCOVERY, DiscoveryProgress.Value.of(++tasksComplete, tasksTotal)), observer);
        }
        if (discoverTopTracks) {
            taskExecutor.execute(new SpotifyTopTracksTaskExecution(discoveryJob, artist, cacheEnabled));
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
            ArtistDiscoveryIndex index = artistDiscoveryIndexRepository.findByArtistId(artist.getId())
                    .orElseGet(() -> new ArtistDiscoveryIndex().setArtist(artist));
            String updatedArtistId = musicBrainzArtistId != null ? musicBrainzArtistId : index.getMusicBrainzArtistId();
            String updatedCountryCode = countryCode != null ? countryCode : index.getCountryCode();
            if (!Objects.equals(index.getMusicBrainzArtistId(), updatedArtistId) ||
                    !Objects.equals(index.getCountryCode(), updatedCountryCode)) {
                artistDiscoveryIndexRepository.save(index.setMusicBrainzArtistId(updatedArtistId).setCountryCode(updatedCountryCode));
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
            ArtistDiscoveryIndex index = artistDiscoveryIndexRepository.findByArtistId(artist.getId())
                    .orElseGet(() -> new ArtistDiscoveryIndex().setArtist(artist));
            String spotifyArtistId = result != null ? result.artist().id() : null;
            artistDiscoveryIndexRepository.save(index.setSpotifyArtistId(spotifyArtistId));
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
}
