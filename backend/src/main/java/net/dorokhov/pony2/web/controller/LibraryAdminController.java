package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.web.dto.*;
import net.dorokhov.pony2.web.dto.ErrorDto.Code;
import net.dorokhov.pony2.web.service.DiscoveryFacade;
import net.dorokhov.pony2.web.service.LibraryFacade;
import net.dorokhov.pony2.web.service.ScanFacade;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class LibraryAdminController implements ErrorHandlingController {

    @ControllerAdvice(assignableTypes = LibraryAdminController.class)
    @ResponseBody
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public static class Advice {

        @ExceptionHandler(ConcurrentLibraryJobException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ErrorDto onConcurrentLibraryJob(ConcurrentLibraryJobException e) {
            return new ErrorDto()
                    .setCode(Code.CONCURRENT_LIBRARY_JOB)
                    .setMessage(e.getMessage());
        }
    }

    private final ScanFacade scanFacade;
    private final DiscoveryFacade discoveryFacade;
    private final LibraryFacade libraryFacade;

    public LibraryAdminController(
            ScanFacade scanFacade,
            DiscoveryFacade discoveryFacade,
            LibraryFacade libraryFacade
    ) {
        this.scanFacade = scanFacade;
        this.discoveryFacade = discoveryFacade;
        this.libraryFacade = libraryFacade;
    }

    @GetMapping("/api/admin/library/scanJobProgress")
    public OptionalResponseDto<ScanJobProgressDto> getCurrentScanJobProgress() {
        return scanFacade.getCurrentScanJobProgress();
    }

    @GetMapping("/api/admin/library/scanJobProgress/{scanJobId}")
    public OptionalResponseDto<ScanJobProgressDto> getScanJobProgress(@PathVariable String scanJobId) {
        return scanFacade.getScanJobProgress(scanJobId);
    }

    @GetMapping("/api/admin/library/scanJobs")
    public ScanJobPageDto getScanJobs(@RequestParam(defaultValue = "0") int pageIndex, @RequestParam(defaultValue = "30") int pageSize) {
        return scanFacade.getScanJobs(pageIndex, pageSize);
    }

    @GetMapping("/api/admin/library/scanJobs/{scanJobId}")
    public ScanJobDto getScanJob(@PathVariable String scanJobId) throws ObjectNotFoundException {
        return scanFacade.getScanJob(scanJobId);
    }

    @PostMapping("/api/admin/library/scanJobs")
    public ScanJobDto startScanJob() throws ConcurrentLibraryJobException {
        return scanFacade.startScanJob();
    }

    @GetMapping("/api/admin/library/discoveryJobProgress")
    public OptionalResponseDto<DiscoveryJobProgressDto> getCurrentDiscoveryJobProgress() {
        return discoveryFacade.getCurrentDiscoveryJobProgress();
    }

    @GetMapping("/api/admin/library/discoveryJobProgress/{discoveryJobId}")
    public OptionalResponseDto<DiscoveryJobProgressDto> getDiscoveryJobProgress(@PathVariable String discoveryJobId) {
        return discoveryFacade.getDiscoveryJobProgress(discoveryJobId);
    }

    @GetMapping("/api/admin/library/discoveryJobs")
    public DiscoveryJobPageDto getDiscoveryJobs(@RequestParam(defaultValue = "0") int pageIndex, @RequestParam(defaultValue = "30") int pageSize) {
        return discoveryFacade.getDiscoveryJobs(pageIndex, pageSize);
    }

    @GetMapping("/api/admin/library/discoveryJobs/{discoveryJobId}")
    public DiscoveryJobDto getDiscoveryJob(@PathVariable String discoveryJobId) throws ObjectNotFoundException {
        return discoveryFacade.getDiscoveryJob(discoveryJobId);
    }

    @PostMapping("/api/admin/library/discoveryJobs/full")
    public DiscoveryJobDto startFullDiscoveryJob(@RequestParam(defaultValue = "true") boolean cacheEnabled) throws ConcurrentLibraryJobException {
        return discoveryFacade.startFullDiscoveryJob(cacheEnabled);
    }

    @PostMapping("/api/admin/library/discoveryJobs/artist/{artistId}")
    public DiscoveryJobDto startArtistDiscoveryJob(@PathVariable String artistId, @RequestParam(defaultValue = "true") boolean cacheEnabled) throws ObjectNotFoundException, ConcurrentLibraryJobException {
        return discoveryFacade.startArtistDiscoveryJob(artistId, cacheEnabled);
    }

    @PostMapping("/api/admin/library/discoveryJobs/album/{albumId}")
    public DiscoveryJobDto startAlbumDiscoveryJob(@PathVariable String albumId, @RequestParam(defaultValue = "true") boolean cacheEnabled) throws ObjectNotFoundException, ConcurrentLibraryJobException {
        return discoveryFacade.startAlbumDiscoveryJob(albumId, cacheEnabled);
    }

    @PostMapping("/api/admin/library/reBuildSearchIndex")
    public void reBuildSearchIndex() {
        libraryFacade.reBuildSearchIndexAsync();
    }

    @PostMapping("/api/admin/library/reGenerateArtworkThumbnails")
    public void reGenerateArtworkThumbnails() {
        libraryFacade.reGenerateArtworkThumbnailsAsync();
    }
}
