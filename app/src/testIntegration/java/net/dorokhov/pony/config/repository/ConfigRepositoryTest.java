package net.dorokhov.pony.config.repository;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.api.config.domain.Config;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigRepositoryTest extends IntegrationTest {

    @Autowired
    private ConfigRepository configRepository;

    @Test
    public void shouldSave() throws Exception {
        Config config = configRepository.save(Config.builder()
                .id("someConfig")
                .value("someValue")
                .build());
        assertThat(configRepository.findOne(config.getId())).isNotNull();
    }
}
