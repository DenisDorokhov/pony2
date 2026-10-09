package net.dorokhov.pony2.core.llm.service;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.config.service.command.ConfigSetUpdateCommand;
import net.dorokhov.pony2.api.llm.service.LlmCacheService;
import net.dorokhov.pony2.core.llm.repository.LlmCacheRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.List;

import static net.dorokhov.pony2.api.llm.domain.LlmCacheRegion.SPOTIFY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmConfigListenerTest extends IntegrationTest {

    @Autowired
    private ConfigService configService;
    @Autowired
    private LlmCacheService llmCacheService;
    @Autowired
    private LlmCacheRepository llmCacheRepository;

    @ParameterizedTest
    @ValueSource(strings = {"url", "model", "key", "removeKey", "disable", "enable"})
    void shouldClearCacheWhenLlmSettingsChange(String change) {
        configService.update(change.equals("enable") ? new ConfigSetUpdateCommand() : config());
        populateCache();
        ConfigSetUpdateCommand updated = switch (change) {
            case "url" -> config().setLlmUrl("http://localhost:11435/v1");
            case "model" -> config().setLlmModel("new-model");
            case "key" -> config().setLlmApiKey("new-key");
            case "removeKey" -> config().setLlmApiKey(null);
            case "disable" -> new ConfigSetUpdateCommand();
            default -> config();
        };

        configService.update(updated);

        assertThat(llmCacheRepository.count()).isZero();
        ConfigSet saved = configService.get();
        assertThat(saved.llmUrl()).isEqualTo(updated.getLlmUrl());
        assertThat(saved.llmModel()).isEqualTo(updated.getLlmModel());
        assertThat(saved.llmApiKey()).isEqualTo(updated.getLlmApiKey());
    }

    @ParameterizedTest
    @ValueSource(strings = {"unchanged", "folders", "emptyValues"})
    void shouldKeepCacheWhenLlmSettingsDoNotChange(String change) {
        configService.update(change.equals("emptyValues") ? new ConfigSetUpdateCommand() : config());
        populateCache();
        ConfigSetUpdateCommand updated = switch (change) {
            case "folders" -> config().setLibraryFolders(List.of(new File("new-library")));
            case "emptyValues" -> new ConfigSetUpdateCommand().setLlmUrl("").setLlmModel(" ").setLlmApiKey("\t");
            default -> config();
        };

        configService.update(updated);

        assertCachePresent();
    }

    @Test
    void shouldRollBackCacheClearWithConfigChange() {
        configService.update(config());
        populateCache();

        getTransactionTemplate().executeWithoutResult(status -> {
            configService.update(config().setLlmModel("new-model"));
            assertThat(configService.get().llmModel()).isEqualTo("new-model");
            assertThat(llmCacheRepository.count()).isZero();
            status.setRollbackOnly();
        });

        assertThat(configService.get().llmModel()).isEqualTo("old-model");
        assertCachePresent();
    }

    @Test
    void shouldKeepCacheWhenConfigIsInvalid() {
        configService.update(config());
        populateCache();

        assertThatThrownBy(() -> configService.update(config().setLlmModel(null)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(configService.get().llmModel()).isEqualTo("old-model");
        assertCachePresent();
    }

    private void populateCache() {
        llmCacheService.put(SPOTIFY, "first", 1, "first-response");
        llmCacheService.put(SPOTIFY, "second", 2, "second-response");
    }

    private void assertCachePresent() {
        assertThat(llmCacheService.get(SPOTIFY, "first", 1)).contains("first-response");
        assertThat(llmCacheService.get(SPOTIFY, "second", 2)).contains("second-response");
    }

    private ConfigSetUpdateCommand config() {
        return new ConfigSetUpdateCommand()
                .setLlmUrl("http://localhost:11434/v1")
                .setLlmModel("old-model")
                .setLlmApiKey("old-key");
    }
}
