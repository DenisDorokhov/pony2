package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.MusicBrainzAlbumData;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.discovery.task.MusicBrainzAlbumDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.ALBUM_DISCOVERY;

@Service
public class AlbumDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final LibraryJobSynchronizer jobSynchronizer;
    private final DiscoveryTaskRepository taskRepository;
    private final MusicBrainzAlbumDataService musicBrainzAlbumDataService;
    private final DiscoveryTaskExecutor taskExecutor;
    private final LogService logService;

    public AlbumDiscoveryService(
            LibraryJobSynchronizer jobSynchronizer,
            DiscoveryTaskRepository taskRepository,
            MusicBrainzAlbumDataService musicBrainzAlbumDataService,
            DiscoveryTaskExecutor taskExecutor,
            LogService logService
    ) {
        this.jobSynchronizer = jobSynchronizer;
        this.taskRepository = taskRepository;
        this.musicBrainzAlbumDataService = musicBrainzAlbumDataService;
        this.taskExecutor = taskExecutor;
        this.logService = logService;
    }

    public void discover(DiscoveryJob discoveryJob, Album album, @Nullable Consumer<DiscoveryProgress> observer) {
        discover(discoveryJob, album, true, observer);
    }

    public void discover(DiscoveryJob discoveryJob, Album album, boolean cacheEnabled, @Nullable Consumer<DiscoveryProgress> observer) {
        try (LibraryJobSynchronizer.DiscoveryTaskRegistration ignored = jobSynchronizer.registerDiscoveryTask()) {
            notifyProgressObserver(new DiscoveryProgress(ALBUM_DISCOVERY, DiscoveryProgress.Value.of(0, 1)), observer);
            taskExecutor.execute(new MusicBrainzTaskExecution(discoveryJob, album, cacheEnabled));
            notifyProgressObserver(new DiscoveryProgress(ALBUM_DISCOVERY, DiscoveryProgress.Value.of(1, 1)), observer);
        }
    }

    private class MusicBrainzTaskExecution implements DiscoveryTaskExecution<MusicBrainzAlbumData> {

        private final DiscoveryJob job;
        private final Album album;
        private final boolean cacheEnabled;

        private MusicBrainzTaskExecution(DiscoveryJob job, Album album, boolean cacheEnabled) {
            this.job = job;
            this.album = album;
            this.cacheEnabled = cacheEnabled;
        }

        @Override
        public DiscoveryTask startTask() {
            return taskRepository.save(new DiscoveryTask()
                    .setJob(job)
                    .setAlbum(album)
                    .setType(DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA)
                    .setStatus(DiscoveryTask.Status.STARTED));
        }

        @Override
        public @Nullable MusicBrainzAlbumData executeTask(DiscoveryTask task) {
            return musicBrainzAlbumDataService.discover(task, cacheEnabled);
        }

        @Override
        public void onError(DiscoveryTask task, Throwable error) {
            Album currentAlbum = requireNonNull(task.getAlbum());
            logService.error(logger, "Could not discover MusicBrainz data for album '{}' -> '{}'.",
                    currentAlbum.getArtist().getName(), currentAlbum.getName(), error);
        }
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
}
