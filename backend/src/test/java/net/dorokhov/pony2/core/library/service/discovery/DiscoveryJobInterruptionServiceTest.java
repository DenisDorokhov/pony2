package net.dorokhov.pony2.core.library.service.discovery;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.repository.DiscoveryJobRepository;
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

    private DiscoveryJob discoveryJob() {
        return new DiscoveryJob()
                .setStatus(DiscoveryJob.Status.STARTING)
                .setType(DiscoveryType.FULL);
    }
}
