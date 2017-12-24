package net.dorokhov.pony.library.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.api.library.domain.ScanJob;
import net.dorokhov.pony.library.repository.ScanJobRepository;
import net.dorokhov.pony.api.log.service.LogService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
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
    public void shouldMarkCurrentJobsAsInterrupted() throws Exception {

        Pageable firstPageable = new PageRequest(0, 2);

        List<Page<ScanJob>> pages = new ArrayList<>();
        pages.add(new PageImpl<>(ImmutableList.of(scanJob(), scanJob()), firstPageable, 3));
        pages.add(new PageImpl<>(ImmutableList.of(scanJob()), new PageRequest(1, 2), 3));
        pages.add(new PageImpl<>(emptyList()));

        when(scanJobRepository.findByStatusIn(any(), any())).then(invocation -> {
            Pageable pageable = invocation.getArgument(1);
            return pages.get(pageable.getPageNumber());
        });

        scanJobInterruptionRunner.markCurrentJobsAsInterrupted();

        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());

        savedScanJob.getAllValues().forEach(scanJob -> assertThat(scanJob.getStatus())
                .isSameAs(ScanJob.Status.INTERRUPTED));
        
        verify(logService).warn(any(), any(), any());
    }

    @Test
    public void shouldMarkCurrentJobsAsInterruptedOnStartup() throws Exception {
        when(scanJobRepository.findByStatusIn(any(), any())).thenReturn(new PageImpl<>(emptyList()));
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