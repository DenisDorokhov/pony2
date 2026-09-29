package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.api.llm.LlmCacheRegion;
import net.dorokhov.pony2.api.llm.service.LlmCacheService;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class LlmAdminController implements ErrorHandlingController {

    private final LlmCacheService llmCacheService;

    public LlmAdminController(LlmCacheService llmCacheService) {
        this.llmCacheService = llmCacheService;
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
