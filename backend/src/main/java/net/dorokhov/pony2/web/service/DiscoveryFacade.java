package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryJobProgress;
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

    public DiscoveryFacade(DiscoveryJobService discoveryJobService, LibraryService libraryService) {
        this.discoveryJobService = discoveryJobService;
        this.libraryService = libraryService;
    }

    @Transactional(readOnly = true)
    public OptionalResponseDto<DiscoveryJobProgressDto> getCurrentDiscoveryJobProgress() {
        DiscoveryJobProgress discoveryJobProgress = discoveryJobService.getCurrentDiscoveryJobProgress().orElse(null);
        if (discoveryJobProgress == null) {
            return OptionalResponseDto.empty();
        }
        return OptionalResponseDto.of(DiscoveryJobProgressDto.of(discoveryJobProgress));
    }

    @Transactional(readOnly = true)
    public OptionalResponseDto<DiscoveryJobProgressDto> getDiscoveryJobProgress(String discoveryJobId) {
        DiscoveryJobProgress discoveryJobProgress = discoveryJobService.getDiscoveryJobProgress(discoveryJobId).orElse(null);
        if (discoveryJobProgress == null) {
            return OptionalResponseDto.empty();
        }
        return OptionalResponseDto.of(DiscoveryJobProgressDto.of(discoveryJobProgress));
    }

    @Transactional(readOnly = true)
    public DiscoveryJobPageDto getDiscoveryJobs(int pageIndex, int pageSize) {
        return DiscoveryJobPageDto.of(discoveryJobService.getAll(PageRequest.of(pageIndex, Math.min(PAGE_SIZE, Math.abs(pageSize)),
                Sort.by(DESC, "creationDate", "updateDate"))));
    }

    @Transactional(readOnly = true)
    public DiscoveryJobDto getDiscoveryJob(String discoveryJobId) throws ObjectNotFoundException {
        DiscoveryJob discoveryJob = discoveryJobService.getById(discoveryJobId).orElse(null);
        if (discoveryJob == null) {
            throw new ObjectNotFoundException(DiscoveryJob.class, discoveryJobId);
        }
        return DiscoveryJobDto.of(discoveryJob);
    }

    @Transactional
    public DiscoveryJobDto startFullDiscoveryJob(boolean cacheEnabled) throws ConcurrentLibraryJobException {
        return DiscoveryJobDto.of(discoveryJobService.startFullJob(cacheEnabled));
    }

    @Transactional
    public DiscoveryJobDto startArtistDiscoveryJob(String artistId, boolean cacheEnabled) throws ObjectNotFoundException, ConcurrentLibraryJobException {
        Artist artist = libraryService.getArtistById(artistId).orElse(null);
        if (artist == null) {
            throw new ObjectNotFoundException(Artist.class, artistId);
        }
        return DiscoveryJobDto.of(discoveryJobService.startArtistJob(artistId, cacheEnabled));
    }

    @Transactional
    public DiscoveryJobDto startAlbumDiscoveryJob(String albumId, boolean cacheEnabled) throws ObjectNotFoundException, ConcurrentLibraryJobException {
        Album album = libraryService.getAlbumById(albumId).orElse(null);
        if (album == null) {
            throw new ObjectNotFoundException(Album.class, albumId);
        }
        return DiscoveryJobDto.of(discoveryJobService.startAlbumJob(albumId, cacheEnabled));
    }
}
