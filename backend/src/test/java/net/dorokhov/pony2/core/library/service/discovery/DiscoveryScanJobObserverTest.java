package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.library.domain.ScanJob;
import net.dorokhov.pony2.api.library.domain.ScanType;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;
import net.dorokhov.pony2.api.library.service.ScanJobService;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.api.log.service.LogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DiscoveryScanJobObserverTest {

    @InjectMocks
    private DiscoveryScanJobObserver discoveryScanJobObserver;

    @Mock
    private ScanJobService scanJobService;
    @Mock
    private DiscoveryJobService discoveryJobService;
    @Mock
    private LogService logService;
    @Mock
    private ConfigService configService;

    @Test
    public void shouldSubscribeAndUnsubscribe() {

        discoveryScanJobObserver.subscribe();
        discoveryScanJobObserver.unsubscribe();

        verify(scanJobService).addObserver(same(discoveryScanJobObserver));
        verify(scanJobService).removeObserver(same(discoveryScanJobObserver));
    }

    @Test
    public void shouldStartFullDiscoveryJobOnFullScanJobCompleted() throws ConcurrentLibraryJobException {

        when(configService.get()).thenReturn(new ConfigSet(null, List.of(), true, "http://localhost", "model", null));
        discoveryScanJobObserver.onScanJobCompleted(scanJob(ScanType.FULL));

        verify(discoveryJobService).startFullJob();
    }

    @Test
    public void shouldStartFullDiscoveryJobOnEditJobCompleted() throws ConcurrentLibraryJobException {

        when(configService.get()).thenReturn(new ConfigSet(null, List.of(), true, "http://localhost", "model", null));
        discoveryScanJobObserver.onScanJobCompleted(scanJob(ScanType.EDIT));

        verify(discoveryJobService).startFullJob();
    }

    @ParameterizedTest
    @EnumSource(ScanType.class)
    public void shouldSkipDiscoveryWhenDiscoveryIsDisabled(ScanType scanType) {
        when(configService.get()).thenReturn(new ConfigSet(null, List.of(), false, "http://localhost/v1", "model", null));

        discoveryScanJobObserver.onScanJobCompleted(scanJob(scanType));

        verifyNoInteractions(discoveryJobService, logService);
    }

    @Test
    public void shouldLogConcurrentLibraryJobException() throws ConcurrentLibraryJobException {

        when(configService.get()).thenReturn(new ConfigSet(null, List.of(), true, "http://localhost", "model", null));
        when(discoveryJobService.startFullJob()).thenThrow(new ConcurrentLibraryJobException());

        discoveryScanJobObserver.onScanJobCompleted(scanJob(ScanType.FULL));

        verify(logService).warn(any(), any(), any());
    }

    private ScanJob scanJob(ScanType scanType) {
        return new ScanJob()
                .setScanType(scanType)
                .setStatus(ScanJob.Status.COMPLETE);
    }
}
