package net.dorokhov.pony.core.config.repository;

import net.dorokhov.pony.api.config.domain.Config;
import net.dorokhov.pony.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigRepositoryTest extends IntegrationTest {

    @Autowired
    private ConfigRepository configRepository;

    @Test
    public void shouldSave() {

        Config config = configRepository.save(Config.builder()
                .id("someConfig")
                .value("someValue")
                .build());

        assertThat(configRepository.findOne(config.getId())).isNotNull();
    }
}
