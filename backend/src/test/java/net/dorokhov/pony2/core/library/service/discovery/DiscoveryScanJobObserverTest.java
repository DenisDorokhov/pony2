package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.api.library.domain.ScanJob;
import net.dorokhov.pony2.api.library.domain.ScanType;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;
import net.dorokhov.pony2.api.library.service.ScanJobService;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.api.log.service.LogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    public void shouldSubscribeAndUnsubscribe() {

        discoveryScanJobObserver.subscribe();
        discoveryScanJobObserver.unsubscribe();

        verify(scanJobService).addObserver(same(discoveryScanJobObserver));
        verify(scanJobService).removeObserver(same(discoveryScanJobObserver));
    }

    @Test
    public void shouldStartFullDiscoveryJobOnFullScanJobCompleted() throws ConcurrentLibraryJobException {

        discoveryScanJobObserver.onScanJobCompleted(scanJob(ScanType.FULL));

        verify(discoveryJobService).startFullJob();
    }

    @Test
    public void shouldNotStartFullDiscoveryJobOnEditJobCompleted() throws ConcurrentLibraryJobException {

        discoveryScanJobObserver.onScanJobCompleted(scanJob(ScanType.EDIT));

        verify(discoveryJobService, never()).startFullJob();
    }

    @Test
    public void shouldLogConcurrentLibraryJobException() throws ConcurrentLibraryJobException {

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
