package net.dorokhov.pony.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.dorokhov.pony.api.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.ErrorDto.Code;
import net.dorokhov.pony.web.domain.ScanJobDto;
import net.dorokhov.pony.web.domain.ScanJobPageDto;
import net.dorokhov.pony.web.domain.ScanJobProgressDto;
import net.dorokhov.pony.web.service.ScanFacade;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static javax.servlet.http.HttpServletResponse.*;
import static net.dorokhov.pony.web.controller.common.SwaggerResponses.FORBIDDEN_MESSAGE;
import static net.dorokhov.pony.web.controller.common.SwaggerResponses.UNAUTHORIZED_MESSAGE;

@RestController
@RequestMapping(value = "/api/admin/library", produces = "application/json")
@Api(tags = "Library Administration")
@ApiResponses({
        @ApiResponse(code = SC_UNAUTHORIZED, message = UNAUTHORIZED_MESSAGE, response = ErrorDto.class),
        @ApiResponse(code = SC_FORBIDDEN, message = FORBIDDEN_MESSAGE, response = ErrorDto.class),
})
public class LibraryAdminController implements ErrorHandlingController {

    @ControllerAdvice(assignableTypes = LibraryAdminController.class)
    @ResponseBody
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public static class Advice {

        @ExceptionHandler(ConcurrentScanException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ErrorDto onConcurrentScan(ConcurrentScanException e) {
            return new ErrorDto(Code.CONCURRENT_SCAN, e.getMessage());
        }
    }

    private final ScanFacade scanFacade;

    public LibraryAdminController(ScanFacade scanFacade) {
        this.scanFacade = scanFacade;
    }

    @GetMapping("/scanJobProgress")
    @ApiOperation("Get current scan job progress.")
    @ApiResponses({
            @ApiResponse(code = SC_NOT_FOUND, message = "No scan job currently running.", response = ErrorDto.class),
    })
    public ScanJobProgressDto getCurrentScanJobProgress() throws ObjectNotFoundException {
        return scanFacade.getCurrentScanJobProgress();
    }

    @GetMapping("/scanJobProgress/{scanJobId}")
    @ApiOperation("Get scan job progress by scan job ID. Used to track scan job progress which was explicitly initiated.")
    @ApiResponses({
            @ApiResponse(code = SC_NOT_FOUND, message = "Requested scan job is not running.", response = ErrorDto.class),
    })
    public ScanJobProgressDto getScanJobProgress(@PathVariable String scanJobId) throws ObjectNotFoundException {
        return scanFacade.getScanJobProgress(scanJobId);
    }

    @GetMapping("/scanJobs")
    @ApiOperation("Get list of scan jobs.")
    public ScanJobPageDto getScanJobs(@RequestParam(defaultValue = "0") int pageIndex) {
        return scanFacade.getScanJobs(pageIndex);
    }

    @GetMapping("/scanJobs/{scanJobId}")
    @ApiOperation("Get scan job details by scan job ID.")
    @ApiResponses({
            @ApiResponse(code = SC_NOT_FOUND, message = "Requested scan job not found.", response = ErrorDto.class),
    })
    public ScanJobDto getScanJob(@PathVariable String scanJobId) throws ObjectNotFoundException {
        return scanFacade.getScanJob(scanJobId);
    }

    @PostMapping("/scanJobs/full")
    @ApiOperation("Start full scan job.")
    @ApiResponses({
            @ApiResponse(code = SC_BAD_REQUEST, message = "Scan job is already running.", response = ErrorDto.class),
    })
    public ScanJobDto startScanJob() throws ConcurrentScanException {
        return scanFacade.startScanJob();
    }
}
