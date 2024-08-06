package net.dorokhov.pony2.core.installation.repository;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.installation.domain.Installation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class InstallationRepositoryTest extends IntegrationTest {

    @Autowired
    private InstallationRepository installationRepository;

    @Test
    public void shouldSave() {

        Installation installation = installationRepository.save(new Installation()
                .setVersion("1.0"));

        assertThat(installationRepository.findById(installation.getId())).isNotEmpty();
    }
}
