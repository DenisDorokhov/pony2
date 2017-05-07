package net.dorokhov.pony.installation.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.installation.domain.Installation;
import net.dorokhov.pony.installation.repository.InstallationRepository;
import net.dorokhov.pony.installation.service.command.InstallationCommand;
import net.dorokhov.pony.user.domain.User.Role;
import net.dorokhov.pony.user.service.command.UserCreationCommand;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class InstallationServiceCacheTest extends IntegrationTest {
    
    @Autowired
    private InstallationService installationService;
    
    @Autowired
    private InstallationRepository installationRepository;

    @Test
    public void shouldCacheAfterFetching() throws Exception {
        installationRepository.save(Installation.builder()
                .version("2.0")
                .build());
        Installation installation = installationService.getInstallation();
        Installation cachedInstallation = installationService.getInstallation();
        assertThat(installation).isSameAs(cachedInstallation);
    }

    @Test
    public void shouldCacheAfterInstallation() throws Exception {
        InstallationCommand command = new InstallationCommand(null, ImmutableList.of(), 
                UserCreationCommand.builder()
                        .name("someName")
                        .email("foo@bar.com")
                        .password("somePassword")
                        .roles(Role.USER, Role.ADMIN)
                        .build());
        Installation installation = installationService.install(command);
        Installation cachedInstallation = installationService.getInstallation();
        assertThat(installation).isSameAs(cachedInstallation);
    }

    @Test
    public void shouldCacheAfterUpgrading() throws Exception {
        installationRepository.save(Installation.builder()
                .version("2.0")
                .build());
        Installation installation = installationService.upgradeIfNeeded();
        Installation cachedInstallation = installationService.getInstallation();
        assertThat(installation).isSameAs(cachedInstallation);
    }
}
