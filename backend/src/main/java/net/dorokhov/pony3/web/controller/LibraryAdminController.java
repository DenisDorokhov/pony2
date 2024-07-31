package net.dorokhov.pony3.web.controller;

import net.dorokhov.pony3.api.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony3.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony3.web.dto.*;
import net.dorokhov.pony3.web.dto.ErrorDto.Code;
import net.dorokhov.pony3.web.service.ScanFacade;
import net.dorokhov.pony3.web.service.exception.ObjectNotFoundException;
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

        @ExceptionHandler(ConcurrentScanException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ErrorDto onConcurrentScan(ConcurrentScanException e) {
            return new ErrorDto()
                    .setCode(Code.CONCURRENT_SCAN)
                    .setMessage(e.getMessage());
        }
    }

    private final ScanFacade scanFacade;

    public LibraryAdminController(ScanFacade scanFacade) {
        this.scanFacade = scanFacade;
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
    public ScanJobDto startScanJob() throws ConcurrentScanException {
        return scanFacade.startScanJob();
    }
}
