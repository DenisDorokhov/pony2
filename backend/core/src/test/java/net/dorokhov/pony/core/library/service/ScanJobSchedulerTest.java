package net.dorokhov.pony.core.library.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.api.config.service.ConfigService;
import net.dorokhov.pony.api.installation.service.InstallationService;
import net.dorokhov.pony.api.library.domain.ScanJob;
import net.dorokhov.pony.api.library.domain.ScanType;
import net.dorokhov.pony.api.library.service.ScanJobService;
import net.dorokhov.pony.api.library.service.exception.ConcurrentScanException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony.test.InstallationFixtures.installation;
import static net.dorokhov.pony.test.ScanJobFixtures.scanJobBuilder;
import static net.dorokhov.pony.test.ScanJobFixtures.scanJobFull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
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
        
        when(installationService.getInstallation()).thenReturn(installation());
        when(configService.getAutoScanInterval()).thenReturn(24 * 60 * 60);
        when(scanJobService.getAll(any())).thenReturn(new PageImpl<>(ImmutableList.of(scanJobBuilder(ScanType.FULL)
                .creationDate(LocalDateTime.now().minusDays(2))
                .updateDate(null)
                .build())));
        ScanJob scanJob = scanJobFull();
        when(scanJobService.startScanJob()).thenReturn(scanJob);
        
        assertThat(scanJobScheduler.startAutoScanJobIfNeeded()).isSameAs(scanJob);
    }

    @Test
    public void shouldStartAutoScanJobIfRunningFirstTime() throws ConcurrentScanException {
        
        when(installationService.getInstallation()).thenReturn(installation());
        when(configService.getAutoScanInterval()).thenReturn(24 * 60 * 60);
        when(scanJobService.getAll(any())).thenReturn(new PageImpl<>(emptyList()));
        ScanJob scanJob = scanJobFull();
        when(scanJobService.startScanJob()).thenReturn(scanJob);
        
        assertThat(scanJobScheduler.startAutoScanJobIfNeeded()).isSameAs(scanJob);
    }

    @Test
    public void shouldSkipAutoScanJobIfAutoScanIsOff() throws ConcurrentScanException {
        
        when(installationService.getInstallation()).thenReturn(installation());
        when(configService.getAutoScanInterval()).thenReturn(null);
        
        assertThat(scanJobScheduler.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobService, never()).startScanJob();
    }

    @Test
    public void shouldSkipAutoScanJobIfLibraryIsAlreadyBeingScanned() throws ConcurrentScanException {
        
        when(installationService.getInstallation()).thenReturn(installation());
        when(configService.getAutoScanInterval()).thenReturn(24 * 60 * 60);
        when(scanJobService.getAll(any())).thenReturn(new PageImpl<>(emptyList()));
        when(scanJobService.startScanJob()).thenThrow(new ConcurrentScanException());
        
        assertThat(scanJobScheduler.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobService).startScanJob();
    }

    @Test
    public void shouldSkipAutoScanJobIfNotInstalled() throws ConcurrentScanException {
        
        when(installationService.getInstallation()).thenReturn(null);
        
        assertThat(scanJobScheduler.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobService, never()).startScanJob();
    }

    @Test
    public void shouldSkipAutoScanJobByInterval() throws ConcurrentScanException {
        
        when(installationService.getInstallation()).thenReturn(installation());
        when(configService.getAutoScanInterval()).thenReturn(24 * 60 * 60);
        when(scanJobService.getAll(any())).thenReturn(new PageImpl<>(ImmutableList.of(scanJobFull())));
        
        assertThat(scanJobScheduler.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobService, never()).startScanJob();
    }
}