package net.dorokhov.pony.library.service.impl;

import net.dorokhov.pony.installation.domain.Installation;
import net.dorokhov.pony.installation.service.InstallationService;
import net.dorokhov.pony.library.service.ScanJobService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ScanJobSchedulerTests {
    
    @InjectMocks
    private ScanJobScheduler scanJobScheduler;

    @Mock
    private InstallationService installationService;
    @Mock
    private ScanJobService scanJobService;

    @Test
    public void startAutoScanJob() throws Exception {
        given(installationService.getInstallation()).willReturn(Installation.builder()
                .version("2.0")
                .build());
        scanJobScheduler.startAutoScanJob();
        verify(scanJobService).startAutoScanJobIfNeeded();
    }

    @Test
    public void skipAutoScanJobIfNotInstalled() throws Exception {
        given(installationService.getInstallation()).willReturn(null);
        scanJobScheduler.startAutoScanJob();
        verify(scanJobService, never()).startAutoScanJobIfNeeded();
    }
}
