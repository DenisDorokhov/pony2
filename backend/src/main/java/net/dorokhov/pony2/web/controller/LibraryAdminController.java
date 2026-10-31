package net.dorokhov.pony2.web.controller;

import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskFilter;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.api.library.domain.DiscoveryCacheRegion;
import net.dorokhov.pony2.api.library.service.DiscoveryCacheService;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.web.dto.*;
import net.dorokhov.pony2.web.dto.ErrorDto.Code;
import net.dorokhov.pony2.web.service.DiscoveryFacade;
import net.dorokhov.pony2.web.service.LibraryFacade;
import net.dorokhov.pony2.web.service.LlmEvaluationExportService;
import net.dorokhov.pony2.web.service.ScanFacade;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Semaphore;

import static net.dorokhov.pony2.web.common.StreamingUtils.isConnectionReset;
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

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Semaphore evaluationDownload = new Semaphore(1);

    private final ScanFacade scanFacade;
    private final DiscoveryFacade discoveryFacade;
    private final LibraryFacade libraryFacade;
    private final DiscoveryCacheService discoveryCacheService;
    private final LlmEvaluationExportService evaluationExportService;
    private final LibraryJobSynchronizer jobSynchronizer;

    public LibraryAdminController(
            ScanFacade scanFacade,
            DiscoveryFacade discoveryFacade,
            LibraryFacade libraryFacade,
            DiscoveryCacheService discoveryCacheService,
            LlmEvaluationExportService evaluationExportService,
            LibraryJobSynchronizer jobSynchronizer
    ) {
        this.scanFacade = scanFacade;
        this.discoveryFacade = discoveryFacade;
        this.libraryFacade = libraryFacade;
        this.discoveryCacheService = discoveryCacheService;
        this.evaluationExportService = evaluationExportService;
        this.jobSynchronizer = jobSynchronizer;
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
    public DiscoveryStatusDto getCurrentDiscoveryJobProgress() {
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

    @GetMapping("/api/admin/library/discoveryJobs/{discoveryJobId}/details")
    public DiscoveryJobDetailsDto getDiscoveryJobDetails(@PathVariable String discoveryJobId) throws ObjectNotFoundException {
        return discoveryFacade.getDiscoveryJobDetails(discoveryJobId);
    }

    @GetMapping("/api/admin/library/discoveryJobs/{discoveryJobId}/tasks")
    public DiscoveryTaskPageDto getDiscoveryTasks(
            @PathVariable String discoveryJobId,
            @RequestParam(required = false) DiscoveryTaskFilter.Result result,
            @RequestParam(required = false) DiscoveryTaskType type,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "30") int pageSize
    ) throws ObjectNotFoundException {
        return discoveryFacade.getDiscoveryTasks(discoveryJobId, new DiscoveryTaskFilter(result, type), pageIndex, pageSize);
    }

    @GetMapping("/api/admin/library/artists/{artistId}/discoveryTasks")
    public DiscoveryTaskPageDto getArtistDiscoveryTasks(
            @PathVariable String artistId,
            @RequestParam(required = false) DiscoveryTaskFilter.Result result,
            @RequestParam(required = false) DiscoveryTaskType type,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "30") int pageSize
    ) throws ObjectNotFoundException {
        return discoveryFacade.getArtistDiscoveryTasks(artistId, new DiscoveryTaskFilter(result, type), pageIndex, pageSize);
    }

    @GetMapping("/api/admin/library/albums/{albumId}/discoveryTasks")
    public DiscoveryTaskPageDto getAlbumDiscoveryTasks(
            @PathVariable String albumId,
            @RequestParam(required = false) DiscoveryTaskFilter.Result result,
            @RequestParam(required = false) DiscoveryTaskType type,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "30") int pageSize
    ) throws ObjectNotFoundException {
        return discoveryFacade.getAlbumDiscoveryTasks(albumId, new DiscoveryTaskFilter(result, type), pageIndex, pageSize);
    }

    @GetMapping("/api/admin/library/discoveryTasks")
    public DiscoveryTaskPageDto getAllDiscoveryTasks(
            @RequestParam(required = false) DiscoveryTaskFilter.Result result,
            @RequestParam(required = false) DiscoveryTaskType type,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "30") int pageSize
    ) {
        return discoveryFacade.getAllDiscoveryTasks(new DiscoveryTaskFilter(result, type), pageIndex, pageSize);
    }

    @GetMapping("/api/admin/library/discoveryTasks/{discoveryTaskId}")
    public DiscoveryTaskDto getDiscoveryTask(@PathVariable String discoveryTaskId) throws ObjectNotFoundException {
        return discoveryFacade.getDiscoveryTask(discoveryTaskId);
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

    @DeleteMapping("/api/admin/discovery/data")
    public void cleanDiscoveryData() throws ConcurrentLibraryJobException {
        discoveryFacade.cleanDiscoveryData();
    }

    @GetMapping("/api/admin/llm/evaluation")
    public void downloadEvaluation(HttpServletResponse response) throws IOException, ConcurrentLibraryJobException {
        if (!evaluationDownload.tryAcquire()) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "An evaluation download is already running.");
            return;
        }
        try (LibraryJobSynchronizer.LibraryJobRegistration ignored = jobSynchronizer.registerMaintenanceJob()) {
            LocalDateTime exportedAt = LocalDateTime.now();
            String timestamp = exportedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
            response.setContentType(APPLICATION_JSON_VALUE);
            response.setHeader("Content-Disposition", "inline; filename=\"pony-evaluation-" + timestamp + ".json\"");
            evaluationExportService.write(response.getOutputStream(), exportedAt);
        } catch (ConcurrentLibraryJobException e) {
            throw e;
        } catch (Throwable t) {
            if (!isConnectionReset(t)) {
                logger.error("Could not export LLM evaluation data.", t);
            }
            // Never append an ErrorDto to a partially written JSON document.
            if (!response.isCommitted()) {
                response.reset();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } finally {
            evaluationDownload.release();
        }
    }

    @DeleteMapping("/api/admin/discovery/cache")
    public void clearCache(@RequestParam(required = false) DiscoveryCacheRegion region) {
        if (region == null) {
            discoveryCacheService.clear();
        } else {
            discoveryCacheService.clear(region);
        }
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
