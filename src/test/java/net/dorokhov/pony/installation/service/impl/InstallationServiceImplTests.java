package net.dorokhov.pony.installation.service.impl;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.config.service.ConfigService;
import net.dorokhov.pony.installation.domain.Installation;
import net.dorokhov.pony.installation.repository.InstallationRepository;
import net.dorokhov.pony.installation.service.command.InstallationCommand;
import net.dorokhov.pony.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.installation.service.exception.NotInstalledException;
import net.dorokhov.pony.installation.service.impl.BuildVersionProvider.BuildVersion;
import net.dorokhov.pony.log.service.LogService;
import net.dorokhov.pony.user.UserService;
import net.dorokhov.pony.user.service.command.UserCreationCommand;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.transaction.support.TransactionSynchronizationManager.*;

@RunWith(MockitoJUnitRunner.class)
public class InstallationServiceImplTests {
    
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
        Installation installation = installationBuilder().build();
        given(installationRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of(installation)));
        assertThat(installationService.getInstallation()).isSameAs(installation);
    }

    @Test
    public void shouldGetNoInstallation() throws Exception {
        given(installationRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of()));
        assertThat(installationService.getInstallation()).isNull();
    }

    @Test
    public void shouldFailWhenMultipleInstallationsDetected() throws Exception {
        Installation installation = installationBuilder().build();
        given(installationRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of(installation, installation)));
        assertThatThrownBy(installationService::getInstallation).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldInstall() throws Exception {
        
        given(installationRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of()));
        given(buildVersionProvider.getBuildVersion()).willReturn(buildVersion());
        Installation installation = installationBuilder().build();
        given(installationRepository.save((Installation) any())).willReturn(installation);
        
        InstallationCommand command = installationCommand();
        assertThat(installationService.install(command)).isSameAs(installation);
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        
        verify(configService).saveAutoScanInterval(command.getAutoScanInterval());
        verify(configService).saveLibraryFolders(command.getLibraryFolders());
        verify(userService).create(command.getUserCreationCommand());
        verify(installationRepository).save((Installation) any());
        verify(logService).info(any(), any(), any());
    }

    @Test
    public void shouldFailWhenAlreadyInstalled() throws Exception {
        Installation installation = installationBuilder().build();
        given(installationRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of(installation)));
        assertThatThrownBy(() -> installationService.install(installationCommand())).isInstanceOf(AlreadyInstalledException.class);
    }

    @Test
    public void shouldFailWhenCouldNotInstall() throws Exception {
        
        given(installationRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of()));
        given(buildVersionProvider.getBuildVersion()).willReturn(buildVersion());
        Exception e = new RuntimeException();
        given(installationRepository.save((Installation) any())).willThrow(e);
        
        InstallationCommand command = new InstallationCommand(null, ImmutableList.of(), userCreationCommand());
        assertThatThrownBy(() -> installationService.install(command)).isSameAs(e);
    }

    @Test
    public void shouldUpgrade() throws Exception {

        Installation installation = installationBuilder().version("2.0").build();
        given(installationRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of(installation)));
        given(buildVersionProvider.getBuildVersion()).willReturn(buildVersion("3.0"));
        given(installationRepository.save((Installation) any())).willReturn(installation);

        assertThat(installationService.upgradeIfNeeded()).isSameAs(installation);
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<Installation> savedInstallation = ArgumentCaptor.forClass(Installation.class);
        verify(installationRepository).save(savedInstallation.capture());
        assertThat(savedInstallation.getValue().getVersion()).isEqualTo("3.0");
        verify(logService).info(any(), any(), eq("2.0"), eq("3.0"));
    }

    @Test
    public void shouldNotUpgradeWhenNotNeeded() throws Exception {

        Installation installation = installationBuilder().version("2.0").build();
        given(installationRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of(installation)));
        given(buildVersionProvider.getBuildVersion()).willReturn(buildVersion("2.0"));
        
        assertThat(installationService.upgradeIfNeeded()).isSameAs(installation);

        verify(installationRepository, never()).save((Installation) any());
    }

    @Test
    public void shouldFailUpgradeWhenNotInstalled() throws Exception {
        given(installationRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of()));
        assertThatThrownBy(installationService::upgradeIfNeeded).isInstanceOf(NotInstalledException.class);
    }

    private Installation.Builder installationBuilder() {
        return Installation.builder().version("2.0");
    }
    
    private BuildVersion buildVersion() {
        return buildVersion("2.0");
    }
    
    private BuildVersion buildVersion(String version) {
        return new BuildVersion(version, LocalDateTime.now());
    }
    
    private InstallationCommand installationCommand() {
        return new InstallationCommand(null, ImmutableList.of(), userCreationCommand());
    }
    
    private UserCreationCommand userCreationCommand() {
        return UserCreationCommand.builder()
                .name("someName")
                .email("someEmail")
                .password("somePassword")
                .build();
    }
}
