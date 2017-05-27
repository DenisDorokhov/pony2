package net.dorokhov.pony.installation.repository;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.installation.domain.Installation;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static net.dorokhov.pony.fixture.InstallationFixtures.installation;
import static org.assertj.core.api.Assertions.assertThat;

public class InstallationRepositoryTest extends IntegrationTest {

    @Autowired
    private InstallationRepository installationRepository;

    @Test
    public void shouldSave() throws Exception {
        Installation installation = installationRepository.save(installation());
        assertThat(installationRepository.findOne(installation.getId())).isNotNull();
    }
}
