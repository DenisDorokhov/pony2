package net.dorokhov.pony2.core.library.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.api.library.domain.ScanJob;
import net.dorokhov.pony2.api.library.domain.ScanType;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.repository.ScanJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScanJobInterruptionServiceTest {

    @InjectMocks
    private ScanJobInterruptionService scanJobInterruptionService;

    @Mock
    private ScanJobRepository scanJobRepository;
    @Mock
    private LogService logService;

    @Test
    public void shouldMarkCurrentJobsAsInterrupted() {

        List<ScanJob> scanJobs = ImmutableList.of(scanJob(), scanJob(), scanJob());

        when(scanJobRepository.findByStatusIn(any())).thenReturn(scanJobs);

        scanJobInterruptionService.markCurrentJobsAsInterrupted();

        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());

        savedScanJob.getAllValues().forEach(scanJob -> assertThat(scanJob.getStatus())
                .isSameAs(ScanJob.Status.INTERRUPTED));

        verify(logService).warn(any(), any(), any());
    }

    private ScanJob scanJob() {
        return new ScanJob()
                .setStatus(ScanJob.Status.STARTING)
                .setScanType(ScanType.FULL);
    }
}