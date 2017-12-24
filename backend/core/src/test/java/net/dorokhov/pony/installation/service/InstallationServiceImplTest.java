package net.dorokhov.pony.installation.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.api.config.service.ConfigService;
import net.dorokhov.pony.api.installation.domain.Installation;
import net.dorokhov.pony.installation.repository.InstallationRepository;
import net.dorokhov.pony.api.installation.service.command.InstallationCommand;
import net.dorokhov.pony.api.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.api.installation.service.exception.NotInstalledException;
import net.dorokhov.pony.installation.service.BuildVersionProvider.BuildVersion;
import net.dorokhov.pony.api.log.service.LogService;
import net.dorokhov.pony.api.user.domain.User.Role;
import net.dorokhov.pony.api.user.service.UserService;
import net.dorokhov.pony.api.user.service.command.UserCreationCommand;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.LocalDateTime;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony.fixture.InstallationFixtures.installation;
import static net.dorokhov.pony.fixture.InstallationFixtures.installationBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.transaction.support.TransactionSynchronizationManager.*;

@RunWith(MockitoJUnitRunner.class)
public class InstallationServiceImplTest {
    
    @InjectMocks
    private InstallationServiceImpl installationService;

    @Mock
    private InstallationRepository installationRepository;
    @Mock
    private BuildVersionProvider buildVersionProvider;
    @Mock
    private ConfigService configService;
    @Mock
    private UserService userService;
    @Mock
    private LogService logService;

    @Before
    public void setUp() throws Exception {
        initSynchronization();
    }

    @After
    public void tearDown() throws Exception {
        clearSynchronization();
    }

    @Test
    public void shouldGetInstallation() throws Exception {
        Installation installation = installation();
        when(installationRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(ImmutableList.of(installation)));
        assertThat(installationService.getInstallation()).isSameAs(installation);
    }

    @Test
    public void shouldGetNoInstallation() throws Exception {
        when(installationRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(emptyList()));
        assertThat(installationService.getInstallation()).isNull();
    }

    @Test
    public void shouldFailWhenMultipleInstallationsDetected() throws Exception {
        Installation installation = installation();
        when(installationRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(ImmutableList.of(installation, installation)));
        assertThatThrownBy(installationService::getInstallation).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldInstall() throws Exception {
        
        when(installationRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(emptyList()));
        when(buildVersionProvider.getBuildVersion()).thenReturn(buildVersion());
        Installation installation = installation();
        when(installationRepository.save((Installation) any())).thenReturn(installation);
        
        InstallationCommand command = installationCommand();
        assertThat(installationService.install(command)).isSameAs(installation);
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        
        verify(configService).saveAutoScanInterval(command.getAutoScanInterval());
        verify(configService).saveLibraryFolders(command.getLibraryFolders());
        verify(installationRepository).save((Installation) any());
        verify(logService).info(any(), any(), any());
        
        ArgumentCaptor<UserCreationCommand> userCreationCommand = ArgumentCaptor.forClass(UserCreationCommand.class);
        verify(userService).create(userCreationCommand.capture());
        assertThat(userCreationCommand.getValue().getName()).isEqualTo("someName");
        assertThat(userCreationCommand.getValue().getEmail()).isEqualTo("someEmail");
        assertThat(userCreationCommand.getValue().getPassword()).isEqualTo("somePassword");
        assertThat(userCreationCommand.getValue().getRoles()).containsExactly(Role.USER, Role.ADMIN);
    }

    @Test
    public void shouldFailWhenAlreadyInstalled() throws Exception {
        Installation installation = installation();
        when(installationRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(ImmutableList.of(installation)));
        assertThatThrownBy(() -> installationService.install(installationCommand())).isInstanceOf(AlreadyInstalledException.class);
    }

    @Test
    public void shouldUpgrade() throws Exception {

        Installation installation = installationBuilder().version("2.0").build();
        when(installationRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(ImmutableList.of(installation)));
        when(buildVersionProvider.getBuildVersion()).thenReturn(buildVersion("3.0"));
        when(installationRepository.save((Installation) any())).thenReturn(installation);

        assertThat(installationService.upgradeIfNeeded()).isSameAs(installation);
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<Installation> savedInstallation = ArgumentCaptor.forClass(Installation.class);
        verify(installationRepository).save(savedInstallation.capture());
        assertThat(savedInstallation.getValue().getVersion()).isEqualTo("3.0");
        verify(logService).info(any(), any(), eq("2.0"), eq("3.0"));
    }

    @Test
    public void shouldNotUpgradeWhenNotNeeded() throws Exception {

        Installation installation = installation();
        when(installationRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(ImmutableList.of(installation)));
        when(buildVersionProvider.getBuildVersion()).thenReturn(buildVersion("2.0"));
        
        assertThat(installationService.upgradeIfNeeded()).isSameAs(installation);

        verify(installationRepository, never()).save((Installation) any());
    }

    @Test
    public void shouldFailUpgradeWhenNotInstalled() throws Exception {
        when(installationRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(emptyList()));
        assertThatThrownBy(installationService::upgradeIfNeeded).isInstanceOf(NotInstalledException.class);
    }
    
    private BuildVersion buildVersion() {
        return buildVersion("2.0");
    }
    
    private BuildVersion buildVersion(String version) {
        return new BuildVersion(version, LocalDateTime.now());
    }
    
    private InstallationCommand installationCommand() {
        return InstallationCommand.builder()
                .adminName("someName")
                .adminEmail("someEmail")
                .adminPassword("somePassword")
                .build();
    }
}
