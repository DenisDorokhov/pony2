package net.dorokhov.pony2.core.llm.service;

import net.dorokhov.pony2.api.config.domain.ConfigChangeEvent;
import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.llm.service.LlmCacheService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
public class LlmConfigListener {

    private final LlmCacheService llmCacheService;

    public LlmConfigListener(LlmCacheService llmCacheService) {
        this.llmCacheService = llmCacheService;
    }

    @Transactional
    @EventListener(ConfigChangeEvent.class)
    public void onConfigChange(ConfigChangeEvent event) {
        ConfigSet oldConfig = event.oldConfig();
        ConfigSet newConfig = event.newConfig();
        if (!Objects.equals(oldConfig.llmUrl(), newConfig.llmUrl())
                || !Objects.equals(oldConfig.llmModel(), newConfig.llmModel())
                || !Objects.equals(oldConfig.llmApiKey(), newConfig.llmApiKey())) {
            llmCacheService.clear();
        }
    }
}
