package net.dorokhov.pony.core.library.service;

import net.dorokhov.pony.api.config.service.ConfigService;
import net.dorokhov.pony.api.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony.core.NoOpTaskExecutor;
import net.dorokhov.pony.api.library.domain.ScanJob;
import net.dorokhov.pony.api.library.domain.ScanJobProgress;
import net.dorokhov.pony.core.library.repository.ScanJobRepository;
import net.dorokhov.pony.core.library.service.scan.LibraryScanner;
import net.dorokhov.pony.api.log.service.LogService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.concurrent.Executor;

import static net.dorokhov.pony.core.PlatformTransactionManagerFixtures.transactionManager;
import static net.dorokhov.pony.test.ScanJobFixtures.scanJobBuilder;
import static net.dorokhov.pony.api.library.domain.ScanType.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.transaction.support.TransactionSynchronizationManager.*;

@RunWith(MockitoJUnitRunner.class)
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

    @Before
    public void setUp() {
        initSynchronization();
    }

    @After
    public void tearDown() {
        clearSynchronization();
    }

    @Test
    public void shouldGetCurrentScanJobProgress() throws ConcurrentScanException {

        assertThat(scanJobService.getCurrentScanJobProgress()).isNull();

        when(scanJobRepository.save((ScanJob) any())).then(returnsFirstArg());

        scanJobService.startScanJob();

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(scanJobService.getCurrentScanJobProgress()).isNotNull();
        assertThat(scanJobService.getCurrentScanJobProgress().getScanJob()).isNotNull();
        assertThat(scanJobService.getCurrentScanJobProgress().getScanProgress()).isNull();
    }

    @Test
    public void shouldGetScanJobProgressById() throws ConcurrentScanException {

        assertThat(scanJobService.getScanJobProgress(1L)).isNull();

        when(scanJobRepository.save((ScanJob) any())).then(invocation -> {
            ScanJob scanJob = invocation.getArgument(0);
            return ScanJob.builder(scanJob).id(1L).build();
        });
        when(scanJobRepository.findOne(2L)).thenReturn(scanJobBuilder(FULL).id(2L).build());

        scanJobService.startScanJob();

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ScanJobProgress currentScanJobProgress = scanJobService.getScanJobProgress(1L);
        assertThat(currentScanJobProgress).isNotNull();
        assertThat(currentScanJobProgress.getScanJob()).isNotNull();
        assertThat(currentScanJobProgress.getScanProgress()).isNull();

        ScanJobProgress otherScanJobProgress = scanJobService.getScanJobProgress(2L);
        assertThat(otherScanJobProgress).isNotNull();
        assertThat(otherScanJobProgress.getScanJob()).isNotNull();
        assertThat(otherScanJobProgress.getScanProgress()).isNull();

        ScanJobProgress notExistingScanJobProgress = scanJobService.getScanJobProgress(3L);
        assertThat(notExistingScanJobProgress).isNull();
    }
}
