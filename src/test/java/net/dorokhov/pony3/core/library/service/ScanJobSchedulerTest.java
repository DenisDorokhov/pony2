package net.dorokhov.pony3.core.library.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony3.api.config.service.ConfigService;
import net.dorokhov.pony3.api.installation.domain.Installation;
import net.dorokhov.pony3.api.installation.service.InstallationService;
import net.dorokhov.pony3.api.library.domain.ScanJob;
import net.dorokhov.pony3.api.library.domain.ScanType;
import net.dorokhov.pony3.api.library.service.ScanJobService;
import net.dorokhov.pony3.api.library.service.exception.ConcurrentScanException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony3.test.ScanJobFixtures.scanJob;
import static net.dorokhov.pony3.test.ScanJobFixtures.scanJobFull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScanJobSchedulerTest {
    
    @InjectMocks
    private ScanJobScheduler scanJobScheduler;
    
    @Mock
    private InstallationService installationService;
    @Mock
    private ConfigService configService;
    @Mock
    private ScanJobService scanJobService;

    @Test
    public void shouldStartAutoScanJobByInterval() throws ConcurrentScanException {
        
        when(installationService.getInstallation()).thenReturn(Optional.of(new Installation()));
        when(configService.getAutoScanInterval()).thenReturn(Optional.of(24 * 60 * 60));
        when(scanJobService.getAll(any())).thenReturn(new PageImpl<>(ImmutableList.of(scanJob(ScanType.FULL)
                .setCreationDate(LocalDateTime.now().minusDays(2))
                .setUpdateDate(null))));
        ScanJob scanJob = scanJobFull();
        when(scanJobService.startScanJob()).thenReturn(scanJob);
        
        assertThat(scanJobScheduler.startAutoScanJobIfNeeded()).containsSame(scanJob);
    }

    @Test
    public void shouldStartAutoScanJobIfRunningFirstTime() throws ConcurrentScanException {
        
        when(installationService.getInstallation()).thenReturn(Optional.of(new Installation()));
        when(configService.getAutoScanInterval()).thenReturn(Optional.of(24 * 60 * 60));
        when(scanJobService.getAll(any())).thenReturn(new PageImpl<>(emptyList()));
        ScanJob scanJob = scanJobFull();
        when(scanJobService.startScanJob()).thenReturn(scanJob);
        
        assertThat(scanJobScheduler.startAutoScanJobIfNeeded()).containsSame(scanJob);
    }

    @Test
    public void shouldSkipAutoScanJobIfAutoScanIsOff() throws ConcurrentScanException {
        
        when(installationService.getInstallation()).thenReturn(Optional.of(new Installation()));
        when(configService.getAutoScanInterval()).thenReturn(Optional.empty());
        
        assertThat(scanJobScheduler.startAutoScanJobIfNeeded()).isEmpty();
        verify(scanJobService, never()).startScanJob();
    }

    @Test
    public void shouldSkipAutoScanJobIfLibraryIsAlreadyBeingScanned() throws ConcurrentScanException {
        
        when(installationService.getInstallation()).thenReturn(Optional.of(new Installation()));
        when(configService.getAutoScanInterval()).thenReturn(Optional.of(24 * 60 * 60));
        when(scanJobService.getAll(any())).thenReturn(new PageImpl<>(emptyList()));
        when(scanJobService.startScanJob()).thenThrow(new ConcurrentScanException());
        
        assertThat(scanJobScheduler.startAutoScanJobIfNeeded()).isEmpty();
        verify(scanJobService).startScanJob();
    }

    @Test
    public void shouldSkipAutoScanJobIfNotInstalled() throws ConcurrentScanException {
        
        when(installationService.getInstallation()).thenReturn(Optional.empty());
        
        assertThat(scanJobScheduler.startAutoScanJobIfNeeded()).isEmpty();
        verify(scanJobService, never()).startScanJob();
    }

    @Test
    public void shouldSkipAutoScanJobByInterval() throws ConcurrentScanException {
        
        when(installationService.getInstallation()).thenReturn(Optional.of(new Installation()));
        when(configService.getAutoScanInterval()).thenReturn(Optional.of(24 * 60 * 60));
        when(scanJobService.getAll(any())).thenReturn(new PageImpl<>(ImmutableList.of(scanJobFull())));
        
        assertThat(scanJobScheduler.startAutoScanJobIfNeeded()).isEmpty();
        verify(scanJobService, never()).startScanJob();
    }
}