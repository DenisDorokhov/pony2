package net.dorokhov.pony2.core.llm.service;

import net.dorokhov.pony2.api.config.domain.ConfigChangeEvent;
import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.llm.service.DiscoveryCacheService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
public class LlmConfigListener {

    private final DiscoveryCacheService discoveryCacheService;

    public LlmConfigListener(DiscoveryCacheService discoveryCacheService) {
        this.discoveryCacheService = discoveryCacheService;
    }

    @Transactional
    @EventListener(ConfigChangeEvent.class)
    public void onConfigChange(ConfigChangeEvent event) {
        ConfigSet oldConfig = event.oldConfig();
        ConfigSet newConfig = event.newConfig();
        if (!Objects.equals(oldConfig.llmUrl(), newConfig.llmUrl())
                || !Objects.equals(oldConfig.llmModel(), newConfig.llmModel())
                || !Objects.equals(oldConfig.llmApiKey(), newConfig.llmApiKey())) {
            discoveryCacheService.clear();
        }
    }
}
