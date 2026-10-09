package net.dorokhov.pony2.web.controller;

import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony2.api.llm.domain.LlmCacheRegion;
import net.dorokhov.pony2.api.llm.service.LlmCacheService;
import net.dorokhov.pony2.web.service.LlmEvaluationExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class LlmAdminController implements ErrorHandlingController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Semaphore evaluationDownload = new Semaphore(1);

    private final LlmCacheService llmCacheService;
    private final LlmEvaluationExportService evaluationExportService;

    public LlmAdminController(LlmCacheService llmCacheService, LlmEvaluationExportService evaluationExportService) {
        this.llmCacheService = llmCacheService;
        this.evaluationExportService = evaluationExportService;
    }

    @GetMapping("/api/admin/llm/evaluation")
    public void downloadEvaluation(HttpServletResponse response) throws IOException {
        if (!evaluationDownload.tryAcquire()) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "An evaluation download is already running.");
            return;
        }
        try {
            LocalDateTime maximumCreationDate = LocalDateTime.now();
            String timestamp = maximumCreationDate.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
            response.setContentType(APPLICATION_JSON_VALUE);
            response.setHeader("Content-Disposition", "attachment; filename=\"pony-evaluation-" + timestamp + ".json\"");
            evaluationExportService.write(response.getOutputStream(), maximumCreationDate);
        } catch (Exception e) {
            if (!isConnectionReset(e)) {
                logger.error("Could not export LLM evaluation data.", e);
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

    @DeleteMapping("/api/admin/llm/cache")
    public void clearCache(@RequestParam(required = false) LlmCacheRegion region) {
        if (region == null) {
            llmCacheService.clear();
        } else {
            llmCacheService.clear(region);
        }
    }
}
