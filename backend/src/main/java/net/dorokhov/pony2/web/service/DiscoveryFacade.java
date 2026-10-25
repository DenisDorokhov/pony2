package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryJobProgress;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskFilter;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;
import net.dorokhov.pony2.api.library.service.LibraryService;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.web.dto.*;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.data.domain.Sort.Direction.DESC;

@Service
public class DiscoveryFacade {

    private static final int PAGE_SIZE = 30;

    private final DiscoveryJobService discoveryJobService;
    private final LibraryService libraryService;
    private final ConfigService configService;

    public DiscoveryFacade(DiscoveryJobService discoveryJobService, LibraryService libraryService, ConfigService configService) {
        this.discoveryJobService = discoveryJobService;
        this.libraryService = libraryService;
        this.configService = configService;
    }

    @Transactional(readOnly = true)
    public DiscoveryStatusDto getCurrentDiscoveryJobProgress() {
        DiscoveryJobProgress discoveryJobProgress = discoveryJobService.getCurrentDiscoveryJobProgress().orElse(null);
        DiscoveryJobProgressDto progress = discoveryJobProgress != null
                ? toDiscoveryJobProgress(discoveryJobProgress).getValue() : null;
        return DiscoveryStatusDto.of(configService.get().discoveryEnabled(), progress);
    }

    private OptionalResponseDto<DiscoveryJobProgressDto> toDiscoveryJobProgress(DiscoveryJobProgress progress) {
        DiscoveryJob job = discoveryJobService.getById(progress.getDiscoveryJob().getId()).orElse(null);
        return job != null ? OptionalResponseDto.of(DiscoveryJobProgressDto.of(progress, toDiscoveryJobDto(job)))
                : OptionalResponseDto.empty();
    }

    private DiscoveryJobDto toDiscoveryJobDto(DiscoveryJob job) {
        Album album = job.getAlbum();
        Artist artist = album != null ? album.getArtist() : job.getArtist();
        return DiscoveryJobDto.of(job,
                job.getGenre() != null ? GenreDto.of(job.getGenre()) : null,
                artist != null ? ArtistDto.of(artist, libraryService.getArtistDiscoveryData(artist.getId()).orElse(null)) : null,
                album != null ? AlbumDto.of(album, libraryService.getAlbumDiscoveryData(album.getId()).orElse(null)) : null,
                job.getSong() != null ? SongDto.of(job.getSong(), true) : null);
    }

    @Transactional(readOnly = true)
    public OptionalResponseDto<DiscoveryJobProgressDto> getDiscoveryJobProgress(String discoveryJobId) {
        DiscoveryJobProgress discoveryJobProgress = discoveryJobService.getDiscoveryJobProgress(discoveryJobId).orElse(null);
        if (discoveryJobProgress == null) {
            return OptionalResponseDto.empty();
        }
        return toDiscoveryJobProgress(discoveryJobProgress);
    }

    @Transactional(readOnly = true)
    public DiscoveryJobPageDto getDiscoveryJobs(int pageIndex, int pageSize) {
        return DiscoveryJobPageDto.of(discoveryJobService.getAll(PageRequest.of(pageIndex, Math.min(PAGE_SIZE, Math.abs(pageSize)),
                Sort.by(DESC, "creationDate", "updateDate"))), this::toDiscoveryJobDto);
    }

    @Transactional(readOnly = true)
    public DiscoveryJobDto getDiscoveryJob(String discoveryJobId) throws ObjectNotFoundException {
        DiscoveryJob discoveryJob = discoveryJobService.getById(discoveryJobId).orElse(null);
        if (discoveryJob == null) {
            throw new ObjectNotFoundException(DiscoveryJob.class, discoveryJobId);
        }
        return toDiscoveryJobDto(discoveryJob);
    }

    @Transactional(readOnly = true)
    public DiscoveryJobDetailsDto getDiscoveryJobDetails(String discoveryJobId) throws ObjectNotFoundException {
        return new DiscoveryJobDetailsDto()
                .setDiscoveryJob(getDiscoveryJob(discoveryJobId))
                .setTaskCount(discoveryJobService.getTaskCount(discoveryJobId));
    }

    @Transactional(readOnly = true)
    public DiscoveryTaskPageDto getAllDiscoveryTasks(DiscoveryTaskFilter filter, int pageIndex, int pageSize) {
        return DiscoveryTaskPageDto.of(discoveryJobService.getAllTasks(filter,
                PageRequest.of(pageIndex, Math.min(PAGE_SIZE, Math.abs(pageSize)))), this::toDiscoveryTaskSummary);
    }

    @Transactional(readOnly = true)
    public DiscoveryTaskPageDto getDiscoveryTasks(String discoveryJobId, DiscoveryTaskFilter filter, int pageIndex, int pageSize) throws ObjectNotFoundException {
        if (discoveryJobService.getById(discoveryJobId).isEmpty()) {
            throw new ObjectNotFoundException(DiscoveryJob.class, discoveryJobId);
        }
        return DiscoveryTaskPageDto.of(discoveryJobService.getJobTasks(discoveryJobId, filter,
                PageRequest.of(pageIndex, Math.min(PAGE_SIZE, Math.abs(pageSize)))), this::toDiscoveryTaskSummary);
    }

    @Transactional(readOnly = true)
    public DiscoveryTaskPageDto getArtistDiscoveryTasks(String artistId, DiscoveryTaskFilter filter, int pageIndex, int pageSize) throws ObjectNotFoundException {
        if (libraryService.getArtistById(artistId).isEmpty()) {
            throw new ObjectNotFoundException(Artist.class, artistId);
        }
        return DiscoveryTaskPageDto.of(discoveryJobService.getArtistTasks(artistId, filter,
                PageRequest.of(pageIndex, Math.min(PAGE_SIZE, Math.abs(pageSize)))), this::toDiscoveryTaskSummary);
    }

    @Transactional(readOnly = true)
    public DiscoveryTaskPageDto getAlbumDiscoveryTasks(String albumId, DiscoveryTaskFilter filter, int pageIndex, int pageSize) throws ObjectNotFoundException {
        if (libraryService.getAlbumById(albumId).isEmpty()) {
            throw new ObjectNotFoundException(Album.class, albumId);
        }
        return DiscoveryTaskPageDto.of(discoveryJobService.getAlbumTasks(albumId, filter,
                PageRequest.of(pageIndex, Math.min(PAGE_SIZE, Math.abs(pageSize)))), this::toDiscoveryTaskSummary);
    }

    @Transactional(readOnly = true)
    public DiscoveryTaskDto getDiscoveryTask(String discoveryTaskId) throws ObjectNotFoundException {
        DiscoveryTask task = discoveryJobService.getTaskById(discoveryTaskId).orElse(null);
        if (task == null) {
            throw new ObjectNotFoundException(DiscoveryTask.class, discoveryTaskId);
        }
        return DiscoveryTaskDto.of(task);
    }

    private DiscoveryTaskSummaryDto toDiscoveryTaskSummary(DiscoveryTask task) {
        Album album = task.getAlbum();
        Artist artist = album != null ? album.getArtist() : task.getArtist();
        return DiscoveryTaskSummaryDto.of(task,
                task.getGenre() != null ? GenreDto.of(task.getGenre()) : null,
                artist != null ? ArtistDto.of(artist, libraryService.getArtistDiscoveryData(artist.getId()).orElse(null)) : null,
                album != null ? AlbumDto.of(album, libraryService.getAlbumDiscoveryData(album.getId()).orElse(null)) : null,
                task.getSong() != null ? SongDto.of(task.getSong(), true) : null);
    }

    public void cleanDiscoveryData() throws ConcurrentLibraryJobException {
        discoveryJobService.cleanDiscoveryData();
    }

    @Transactional
    public DiscoveryJobDto startFullDiscoveryJob(boolean cacheEnabled) throws ConcurrentLibraryJobException {
        return toDiscoveryJobDto(discoveryJobService.startFullJob(cacheEnabled));
    }

    @Transactional
    public DiscoveryJobDto startArtistDiscoveryJob(String artistId, boolean cacheEnabled) throws ObjectNotFoundException, ConcurrentLibraryJobException {
        Artist artist = libraryService.getArtistById(artistId).orElse(null);
        if (artist == null) {
            throw new ObjectNotFoundException(Artist.class, artistId);
        }
        return toDiscoveryJobDto(discoveryJobService.startArtistJob(artistId, cacheEnabled));
    }

    @Transactional
    public DiscoveryJobDto startAlbumDiscoveryJob(String albumId, boolean cacheEnabled) throws ObjectNotFoundException, ConcurrentLibraryJobException {
        Album album = libraryService.getAlbumById(albumId).orElse(null);
        if (album == null) {
            throw new ObjectNotFoundException(Album.class, albumId);
        }
        return toDiscoveryJobDto(discoveryJobService.startAlbumJob(albumId, cacheEnabled));
    }
}
