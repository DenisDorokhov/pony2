package net.dorokhov.pony3.core.installation.repository;

import net.dorokhov.pony3.IntegrationTest;
import net.dorokhov.pony3.api.installation.domain.Installation;
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
