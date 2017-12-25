package net.dorokhov.pony.core.installation.service;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.api.installation.domain.Installation;
import net.dorokhov.pony.api.installation.service.InstallationService;
import net.dorokhov.pony.core.installation.repository.InstallationRepository;
import net.dorokhov.pony.api.installation.service.command.InstallationCommand;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static net.dorokhov.pony.test.InstallationFixtures.installation;
import static org.assertj.core.api.Assertions.assertThat;

public class InstallationServiceCacheTest extends IntegrationTest {

    @Autowired
    private InstallationService installationService;

    @Autowired
    private InstallationRepository installationRepository;

    @Test
    public void shouldCacheAfterFetching() throws Exception {
        installationRepository.save(installation());
        Installation installation = installationService.getInstallation();
        Installation cachedInstallation = installationService.getInstallation();
        assertThat(installation).isSameAs(cachedInstallation);
    }

    @Test
    public void shouldEvictCacheAfterInstallation() throws Exception {
        InstallationCommand command = InstallationCommand.builder()
                .adminName("someName")
                .adminEmail("foo@bar.com")
                .adminPassword("somePassword")
                .build();
        Installation installation = installationService.install(command);
        Installation cachedInstallation = installationService.getInstallation();
        assertThat(installation).isNotSameAs(cachedInstallation);
    }

    @Test
    public void shouldEvictCacheAfterUpgrading() throws Exception {
        installationRepository.save(installation());
        Installation installation = installationService.upgradeIfNeeded();
        Installation cachedInstallation = installationService.getInstallation();
        assertThat(installation).isNotSameAs(cachedInstallation);
    }
}
