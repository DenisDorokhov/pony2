package net.dorokhov.pony.core.library.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.api.library.domain.ScanJob;
import net.dorokhov.pony.api.library.domain.ScanType;
import net.dorokhov.pony.api.log.service.LogService;
import net.dorokhov.pony.core.library.repository.ScanJobRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ScanJobInterruptionRunnerTest {

    @InjectMocks
    private ScanJobInterruptionRunner scanJobInterruptionRunner;

    @Mock
    private ScanJobRepository scanJobRepository;
    @Mock
    private LogService logService;

    @Test
    public void shouldMarkCurrentJobsAsInterrupted() {

        List<ScanJob> scanJobs = ImmutableList.of(scanJob(), scanJob(), scanJob());

        when(scanJobRepository.findByStatusIn(any())).thenReturn(scanJobs);

        scanJobInterruptionRunner.markCurrentJobsAsInterrupted();

        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());

        savedScanJob.getAllValues().forEach(scanJob -> assertThat(scanJob.getStatus())
                .isSameAs(ScanJob.Status.INTERRUPTED));

        verify(logService).warn(any(), any(), any());
    }

    @Test
    public void shouldMarkCurrentJobsAsInterruptedOnStartup() {

        when(scanJobRepository.findByStatusIn(any())).thenReturn(emptyList());
        ScanJobInterruptionRunner spy = Mockito.spy(scanJobInterruptionRunner);

        spy.run(new DefaultApplicationArguments(new String[0]));

        verify(spy).markCurrentJobsAsInterrupted();
    }

    private ScanJob scanJob() {
        return ScanJob.builder()
                .status(ScanJob.Status.STARTING)
                .scanType(ScanType.FULL)
                .build();
    }
}