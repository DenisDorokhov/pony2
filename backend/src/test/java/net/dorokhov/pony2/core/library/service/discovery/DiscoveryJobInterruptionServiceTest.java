package net.dorokhov.pony2.core.library.service.discovery;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.repository.DiscoveryJobRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
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
public class DiscoveryJobInterruptionServiceTest {

    @InjectMocks
    private DiscoveryJobInterruptionService discoveryJobInterruptionService;

    @Mock
    private DiscoveryJobRepository discoveryJobRepository;
    @Mock
    private DiscoveryTaskRepository discoveryTaskRepository;
    @Mock
    private LogService logService;

    @Test
    public void shouldMarkCurrentJobsAsInterrupted() {

        List<DiscoveryJob> discoveryJobs = ImmutableList.of(discoveryJob(), discoveryJob(), discoveryJob());

        when(discoveryJobRepository.findByStatusIn(any())).thenReturn(discoveryJobs);

        discoveryJobInterruptionService.markCurrentJobsAsInterrupted();

        ArgumentCaptor<DiscoveryJob> savedDiscoveryJob = ArgumentCaptor.forClass(DiscoveryJob.class);
        verify(discoveryJobRepository, times(3)).save(savedDiscoveryJob.capture());

        savedDiscoveryJob.getAllValues().forEach(discoveryJob -> assertThat(discoveryJob.getStatus())
                .isSameAs(DiscoveryJob.Status.INTERRUPTED));

        verify(logService).warn(any(), any(), any());
    }

    @Test
    public void shouldMarkCurrentTasksAsInterrupted() {

        DiscoveryTask task = new DiscoveryTask().setStatus(DiscoveryTask.Status.STARTED);
        when(discoveryTaskRepository.findByStatus(DiscoveryTask.Status.STARTED)).thenReturn(List.of(task));

        discoveryJobInterruptionService.markCurrentJobsAsInterrupted();

        verify(discoveryTaskRepository).save(task);
        assertThat(task.getStatus()).isSameAs(DiscoveryTask.Status.INTERRUPTED);
        assertThat(task.getResult()).isNull();
        verify(logService).warn(any(), eq("Interrupted {} discovery task(s)."), eq(1));
    }

    @Test
    public void shouldNotLogWhenNothingWasInterrupted() {

        discoveryJobInterruptionService.markCurrentJobsAsInterrupted();

        verify(discoveryJobRepository, never()).save(any());
        verify(discoveryTaskRepository, never()).save(any());
        verifyNoInteractions(logService);
    }

    private DiscoveryJob discoveryJob() {
        return new DiscoveryJob()
                .setStatus(DiscoveryJob.Status.STARTING)
                .setType(DiscoveryType.FULL);
    }
}
