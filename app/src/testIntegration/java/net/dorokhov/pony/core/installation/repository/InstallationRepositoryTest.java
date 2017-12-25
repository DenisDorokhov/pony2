package net.dorokhov.pony.core.installation.repository;

import net.dorokhov.pony.api.installation.domain.Installation;
import net.dorokhov.pony.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static net.dorokhov.pony.test.InstallationFixtures.installation;
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
