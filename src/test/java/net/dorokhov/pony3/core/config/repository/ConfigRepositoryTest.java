package net.dorokhov.pony3.core.config.repository;

import net.dorokhov.pony3.IntegrationTest;
import net.dorokhov.pony3.api.config.domain.Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ConfigRepositoryTest extends IntegrationTest {

    @Autowired
    private ConfigRepository configRepository;

    @Test
    public void shouldSave() {

        Config config = configRepository.save(new Config()
                .setId("someConfig")
                .setValue("someValue"));

        assertThat(configRepository.findById(config.getId())).isNotEmpty();
    }
}
