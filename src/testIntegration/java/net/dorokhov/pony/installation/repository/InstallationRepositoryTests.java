package net.dorokhov.pony.installation.repository;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.installation.domain.Installation;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class InstallationRepositoryTests extends IntegrationTest {

    @Autowired
    private InstallationRepository installationRepository;

    @Test
    public void save() throws Exception {
        Installation installation = installationRepository.save(Installation.builder()
                .version("1.0")
                .build());
        assertThat(installationRepository.findOne(installation.getId())).isNotNull();
    }

}
