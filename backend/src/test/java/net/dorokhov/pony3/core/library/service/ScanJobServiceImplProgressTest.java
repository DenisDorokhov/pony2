package net.dorokhov.pony3.core.library.service;

import net.dorokhov.pony3.api.config.service.ConfigService;
import net.dorokhov.pony3.api.library.domain.ScanJob;
import net.dorokhov.pony3.api.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony3.api.log.service.LogService;
import net.dorokhov.pony3.core.library.NoOpTaskExecutor;
import net.dorokhov.pony3.core.library.repository.ScanJobRepository;
import net.dorokhov.pony3.core.library.service.scan.LibraryScanner;
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

import static net.dorokhov.pony3.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.transaction.support.TransactionSynchronizationManager.*;

@ExtendWith(MockitoExtension.class)
public class ScanJobServiceImplProgressTest {

    @InjectMocks
    private ScanJobServiceImpl scanJobService;

    @Mock
    private ScanJobRepository scanJobRepository;
    @Mock
    @SuppressWarnings("unused")
    private ConfigService configService;
    @Mock
    @SuppressWarnings("unused")
    private LibraryScanner libraryScanner;
    @Mock
    @SuppressWarnings("unused")
    private LogService logService;

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
    public void shouldGetCurrentScanJobProgress() throws ConcurrentScanException {

        assertThat(scanJobService.getCurrentScanJobProgress()).isEmpty();

        when(scanJobRepository.save(any())).then(returnsFirstArg());

        scanJobService.startScanJob();

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(scanJobService.getCurrentScanJobProgress()).hasValueSatisfying(scanJobProgress -> {
            assertThat(scanJobProgress.getScanJob()).isNotNull();
            assertThat(scanJobProgress.getScanProgress()).isNull();
        });
    }

    @Test
    public void shouldGetScanJobProgressById() throws ConcurrentScanException {

        assertThat(scanJobService.getScanJobProgress("1")).isEmpty();

        when(scanJobRepository.save(any())).then(invocation -> {
            ScanJob scanJob = invocation.getArgument(0);
            return scanJob.setId("1");
        });

        scanJobService.startScanJob();

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(scanJobService.getScanJobProgress("1")).hasValueSatisfying(currentScanJobProgress -> {
            assertThat(currentScanJobProgress.getScanJob()).isNotNull();
            assertThat(currentScanJobProgress.getScanProgress()).isNull();
        });

        assertThat(scanJobService.getScanJobProgress("2")).isEmpty();
        assertThat(scanJobService.getScanJobProgress("3")).isEmpty();
    }
}
