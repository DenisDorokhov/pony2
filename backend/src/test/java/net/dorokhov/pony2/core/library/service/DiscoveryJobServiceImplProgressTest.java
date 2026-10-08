package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentDiscoveryException;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.NoOpTaskExecutor;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.core.library.service.discovery.AlbumDiscoveryService;
import net.dorokhov.pony2.core.library.service.discovery.ArtistDiscoveryService;
import net.dorokhov.pony2.core.library.service.discovery.FullDiscoveryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.concurrent.Executor;

import static net.dorokhov.pony2.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.transaction.support.TransactionSynchronizationManager.*;

@ExtendWith(MockitoExtension.class)
public class DiscoveryJobServiceImplProgressTest {

    @InjectMocks
    private DiscoveryJobServiceImpl discoveryJobService;

    @Mock
    private DiscoveryJobRepository discoveryJobRepository;
    @Mock
    @SuppressWarnings("unused")
    private DiscoveryTaskRepository discoveryTaskRepository;
    @Mock
    @SuppressWarnings("unused")
    private ArtistRepository artistRepository;
    @Mock
    @SuppressWarnings("unused")
    private AlbumRepository albumRepository;
    @Mock
    @SuppressWarnings("unused")
    private FullDiscoveryService fullDiscoveryService;
    @Mock
    @SuppressWarnings("unused")
    private ArtistDiscoveryService artistDiscoveryService;
    @Mock
    @SuppressWarnings("unused")
    private AlbumDiscoveryService albumDiscoveryService;
    @Mock
    @SuppressWarnings("unused")
    private LogService logService;

    @Spy
    @SuppressWarnings("unused")
    private final LibraryJobLockService libraryJobLockService = new LibraryJobLockService();

    @Spy
    @SuppressWarnings("unused")
    private final Executor executor = new NoOpTaskExecutor();
    @Spy
    @SuppressWarnings("unused")
    private final PlatformTransactionManager transactionManager = transactionManager();

    @BeforeEach
    public void setUp() {
        initSynchronization();
    }

    @AfterEach
    public void tearDown() {
        clearSynchronization();
    }

    @Test
    public void shouldGetCurrentDiscoveryJobProgress() throws ConcurrentDiscoveryException {

        assertThat(discoveryJobService.getCurrentDiscoveryJobProgress()).isEmpty();

        when(discoveryJobRepository.save(any())).then(returnsFirstArg());

        discoveryJobService.startFullJob();

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(discoveryJobService.getCurrentDiscoveryJobProgress()).hasValueSatisfying(discoveryJobProgress -> {
            assertThat(discoveryJobProgress.getDiscoveryJob()).isNotNull();
            assertThat(discoveryJobProgress.getDiscoveryJob().getStatus()).isSameAs(DiscoveryJob.Status.STARTING);
            assertThat(discoveryJobProgress.getDiscoveryProgress()).isNull();
        });
    }

    @Test
    public void shouldGetDiscoveryJobProgressById() throws ConcurrentDiscoveryException {

        assertThat(discoveryJobService.getDiscoveryJobProgress("1")).isEmpty();

        when(discoveryJobRepository.save(any())).then(invocation -> {
            DiscoveryJob discoveryJob = invocation.getArgument(0);
            return discoveryJob.setId("1");
        });

        discoveryJobService.startFullJob();

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(discoveryJobService.getDiscoveryJobProgress("1")).hasValueSatisfying(discoveryJobProgress -> {
            assertThat(discoveryJobProgress.getDiscoveryJob()).isNotNull();
            assertThat(discoveryJobProgress.getDiscoveryJob().getStatus()).isSameAs(DiscoveryJob.Status.STARTING);
            assertThat(discoveryJobProgress.getDiscoveryProgress()).isNull();
        });

        assertThat(discoveryJobService.getDiscoveryJobProgress("2")).isEmpty();
        assertThat(discoveryJobService.getDiscoveryJobProgress("3")).isEmpty();
    }
}
