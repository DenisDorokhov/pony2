package net.dorokhov.pony.library.service.impl;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.config.service.ConfigService;
import net.dorokhov.pony.library.domain.*;
import net.dorokhov.pony.library.repository.ScanJobRepository;
import net.dorokhov.pony.library.service.ScanJobService;
import net.dorokhov.pony.library.service.command.EditCommand;
import net.dorokhov.pony.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony.library.service.exception.SongNotFoundException;
import net.dorokhov.pony.library.service.impl.audio.domain.WritableAudioData;
import net.dorokhov.pony.library.service.impl.scan.LibraryScanner;
import net.dorokhov.pony.log.domain.LogMessage;
import net.dorokhov.pony.log.service.LogService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony.fixture.PlatformTransactionManagerFixtures.transactionManager;
import static net.dorokhov.pony.fixture.ScanJobFixtures.scanJobEdit;
import static net.dorokhov.pony.fixture.ScanJobFixtures.scanJobFull;
import static net.dorokhov.pony.fixture.ScanResultFixtures.scanResult;
import static net.dorokhov.pony.library.domain.ScanJob.Status.*;
import static net.dorokhov.pony.library.domain.ScanProgress.Step.EDIT_PREPARING;
import static net.dorokhov.pony.library.domain.ScanProgress.Step.FULL_PREPARING;
import static net.dorokhov.pony.library.domain.ScanType.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.transaction.support.TransactionSynchronizationManager.*;

@RunWith(MockitoJUnitRunner.class)
public class ScanJobServiceImplTest {
    
    @InjectMocks
    private ScanJobServiceImpl scanJobService;

    @Mock
    private ScanJobRepository scanJobRepository;
    @Mock
    private ConfigService configService;
    @Mock
    private LibraryScanner libraryScanner;
    @Mock
    private LogService logService;
    
    @Spy
    @SuppressWarnings("unused")
    private final Executor executor = new SyncTaskExecutor();
    @Spy
    @SuppressWarnings("unused")
    private final PlatformTransactionManager transactionManager = transactionManager();

    @Before
    public void setUp() throws Exception {
        initSynchronization();
    }

    @After
    public void tearDown() throws Exception {
        clearSynchronization();
    }

    @Test
    public void shouldGetAll() throws Exception {
        Page<ScanJob> page = new PageImpl<>(emptyList());
        when(scanJobRepository.findAll((Pageable) any())).thenReturn(page);
        assertThat(scanJobService.getAll(new PageRequest(0, 10))).isSameAs(page);
    }

    @Test
    public void shouldGetById() throws Exception {
        ScanJob scanJob = scanJobFull();
        when(scanJobRepository.findOne(any())).thenReturn(scanJob);
        assertThat(scanJobService.getById(1L)).isSameAs(scanJob);
    }

    @Test
    public void shouldExecuteScanJob() throws Exception {
        
        when(configService.getLibraryFolders()).thenReturn(ImmutableList.of(new File("someFolder")));
        when(logService.info(any(), any(), any())).thenReturn(logMessage());
        when(scanJobRepository.save((ScanJob) any())).then(returnsFirstArg());
        ScanResult scanResult = scanResult(FULL);
        when(libraryScanner.scan(any(), any())).then(invocation -> {
            Consumer<ScanProgress> observer = invocation.getArgument(1);
            observer.accept(new ScanProgress(FULL_PREPARING, emptyList(), 0.5));
            return scanResult;
        });
        
        ScanJobServiceObserver observer = new ScanJobServiceObserver();
        scanJobService.addObserver(observer);
        
        ScanJob scanJobStarting = scanJobService.startScanJob();
        assertThat(scanJobStarting.getScanType()).isEqualTo(FULL);
        assertThat(scanJobStarting.getStatus()).isEqualTo(STARTING);
        assertThat(scanJobStarting.getLogMessage()).isNotNull();
        assertThat(scanJobStarting.getScanResult()).isNull();
        verify(logService).info(any(), any(), any());

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        
        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());
        verify(logService, times(3)).info(any(), any(), any());

        ScanJob scanJobStarted = savedScanJob.getAllValues().get(1);
        ScanJob scanJobComplete = savedScanJob.getAllValues().get(2);
        
        assertThat(scanJobStarted.getScanType()).isEqualTo(FULL);
        assertThat(scanJobStarted.getStatus()).isEqualTo(STARTED);
        assertThat(scanJobStarted.getLogMessage()).isNotNull();
        assertThat(scanJobStarted.getScanResult()).isNull();
        
        assertThat(scanJobComplete.getScanType()).isEqualTo(FULL);
        assertThat(scanJobComplete.getStatus()).isEqualTo(ScanJob.Status.COMPLETE);
        assertThat(scanJobComplete.getLogMessage()).isNotNull();
        assertThat(scanJobComplete.getScanResult()).isSameAs(scanResult);
        
        assertThat(observer.getCallCount()).isEqualTo(4);
        observer.assertThatStartingAt(0);
        observer.assertThatStartedAt(1);
        observer.assertThatProgressedAt(2, scanJobProgress -> {
            //noinspection ConstantConditions
            assertThat(scanJobProgress.getScanProgress().getStep()).isEqualTo(FULL_PREPARING);
            assertThat(scanJobProgress.getScanProgress().getFiles()).isEmpty();
            assertThat(scanJobProgress.getScanProgress().getValue()).isEqualTo(0.5);
        });
        observer.assertThatCompletedAt(3);

        scanJobService.removeObserver(observer);

        scanJobService.startScanJob();
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(observer.getCallCount()).isEqualTo(4);
    }

    @Test
    public void shouldExecuteEditJob() throws Exception {

        when(logService.info(any(), any(), any())).thenReturn(logMessage());
        when(scanJobRepository.save((ScanJob) any())).then(returnsFirstArg());
        ScanResult scanResult = scanResult(ScanType.EDIT);
        when(libraryScanner.edit(any(), any())).then(invocation -> {
            Consumer<ScanProgress> observer = invocation.getArgument(1);
            observer.accept(new ScanProgress(EDIT_PREPARING, emptyList(), 0.5));
            return scanResult;
        });

        ScanJobServiceObserver observer = new ScanJobServiceObserver();
        scanJobService.addObserver(observer);

        ScanJob scanJobStarting = scanJobService.startEditJob(ImmutableList.of(new EditCommand(1L, writableAudioData())));
        assertThat(scanJobStarting.getScanType()).isEqualTo(ScanType.EDIT);
        assertThat(scanJobStarting.getStatus()).isEqualTo(STARTING);
        assertThat(scanJobStarting.getLogMessage()).isNotNull();
        assertThat(scanJobStarting.getScanResult()).isNull();
        verify(logService).info(any(), any(), any());

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());
        verify(logService, times(3)).info(any(), any(), any());

        ScanJob scanJobStarted = savedScanJob.getAllValues().get(1);
        ScanJob scanJobComplete = savedScanJob.getAllValues().get(2);

        assertThat(scanJobStarted.getScanType()).isEqualTo(ScanType.EDIT);
        assertThat(scanJobStarted.getStatus()).isEqualTo(STARTED);
        assertThat(scanJobStarted.getLogMessage()).isNotNull();
        assertThat(scanJobStarted.getScanResult()).isNull();

        assertThat(scanJobComplete.getScanType()).isEqualTo(ScanType.EDIT);
        assertThat(scanJobComplete.getStatus()).isEqualTo(ScanJob.Status.COMPLETE);
        assertThat(scanJobComplete.getLogMessage()).isNotNull();
        assertThat(scanJobComplete.getScanResult()).isSameAs(scanResult);

        assertThat(observer.getCallCount()).isEqualTo(4);
        observer.assertThatStartingAt(0);
        observer.assertThatStartedAt(1);
        observer.assertThatProgressedAt(2, scanJobProgress -> {
            //noinspection ConstantConditions
            assertThat(scanJobProgress.getScanProgress().getStep()).isEqualTo(EDIT_PREPARING);
            assertThat(scanJobProgress.getScanProgress().getFiles()).isEmpty();
            assertThat(scanJobProgress.getScanProgress().getValue()).isEqualTo(0.5);
        });
        observer.assertThatCompletedAt(3);

        scanJobService.removeObserver(observer);

        scanJobService.startScanJob();
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(observer.getCallCount()).isEqualTo(4);
    }

    @Test
    public void shouldFailScanJobOnIOException() throws Exception {
        doTestFailScanJobOnException(new IOException());
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldFailScanJobOnConcurrentScanException() throws Exception {

        when(scanJobRepository.save((ScanJob) any())).then(returnsFirstArg());
        scanJobService.startScanJob();
        
        assertThatThrownBy(() -> scanJobService.startScanJob()).isInstanceOf(ConcurrentScanException.class);
        
        AtomicBoolean isConcurrentScan = new AtomicBoolean(false);
        Thread thread = new Thread(() -> {
            try {
                scanJobService.startScanJob();
            } catch (ConcurrentScanException e) {
                isConcurrentScan.set(true);
            }
        });
        thread.start();
        thread.join();
        assertThat(isConcurrentScan.get()).isTrue();
    }

    @Test
    public void shouldFailScanJobOnUnexpectedException() throws Exception {
        doTestFailScanJobOnException(new RuntimeException());
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldFailEditJobOnSongNotFoundException() throws Exception {
        doTestFailEditJobOnException(new SongNotFoundException(1L));
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldFailEditJobOnIOException() throws Exception {
        doTestFailEditJobOnException(new IOException());
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldFailEditJobOnConcurrentScanException() throws Exception {

        when(scanJobRepository.save((ScanJob) any())).then(returnsFirstArg());
        scanJobService.startEditJob(emptyList());
        
        assertThatThrownBy(() -> scanJobService.startEditJob(emptyList())).isInstanceOf(ConcurrentScanException.class);

        AtomicBoolean isConcurrentScan = new AtomicBoolean(false);
        Thread thread = new Thread(() -> {
            try {
                scanJobService.startEditJob(emptyList());
            } catch (ConcurrentScanException e) {
                isConcurrentScan.set(true);
            }
        });
        thread.start();
        thread.join();
        assertThat(isConcurrentScan.get()).isTrue();
    }

    @Test
    public void shouldFailEditJobOnUnexpectedException() throws Exception {
        doTestFailEditJobOnException(new RuntimeException());
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldIgnoreExceptionsThrownByObservers() throws Exception {
        
        when(scanJobRepository.save((ScanJob) any())).then(returnsFirstArg());
        
        ThrowingScanJobServiceObserver observer = new ThrowingScanJobServiceObserver();
        scanJobService.addObserver(observer);
        
        scanJobService.startScanJob();
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        
        assertThat(observer.getCallCount()).isEqualTo(3);
    }

    private void doTestFailScanJobOnException(Exception e) throws Exception {
        
        when(configService.getLibraryFolders()).thenReturn(ImmutableList.of(new File("someFolder")));
        when(logService.error(any(), any(), any())).thenReturn(logMessage());
        when(scanJobRepository.save((ScanJob) any())).then(returnsFirstArg());
        when(scanJobRepository.findOne(any())).thenReturn(scanJobFull());
        when(libraryScanner.scan(any(), any())).thenThrow(e);

        ScanJobServiceObserver observer = new ScanJobServiceObserver();
        scanJobService.addObserver(observer);
        
        scanJobService.startScanJob();
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());
        verify(logService, times(2)).info(any(), any(), any());

        ScanJob scanJobFailed = savedScanJob.getValue();
        assertThat(scanJobFailed.getScanType()).isEqualTo(FULL);
        assertThat(scanJobFailed.getStatus()).isEqualTo(ScanJob.Status.FAILED);
        assertThat(scanJobFailed.getLogMessage()).isNotNull();
        assertThat(scanJobFailed.getScanResult()).isNull();
        
        assertThat(observer.getCallCount()).isEqualTo(3);
        observer.assertThatStartingAt(0);
        observer.assertThatStartedAt(1);
        observer.assertThatFailedAt(2);
    }
    
    private void doTestFailEditJobOnException(Exception e) throws Exception {
        
        when(logService.error(any(), any(), any())).thenReturn(logMessage());
        when(scanJobRepository.save((ScanJob) any())).then(returnsFirstArg());
        when(scanJobRepository.findOne(any())).thenReturn(scanJobEdit());
        when(libraryScanner.edit(any(), any())).thenThrow(e);

        ScanJobServiceObserver observer = new ScanJobServiceObserver();
        scanJobService.addObserver(observer);

        scanJobService.startEditJob(ImmutableList.of(new EditCommand(1L, writableAudioData())));
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());
        verify(logService, times(2)).info(any(), any(), any());

        ScanJob scanJobFailed = savedScanJob.getValue();
        assertThat(scanJobFailed.getScanType()).isEqualTo(ScanType.EDIT);
        assertThat(scanJobFailed.getStatus()).isEqualTo(ScanJob.Status.FAILED);
        assertThat(scanJobFailed.getLogMessage()).isNotNull();
        assertThat(scanJobFailed.getScanResult()).isNull();

        assertThat(observer.getCallCount()).isEqualTo(3);
        observer.assertThatStartingAt(0);
        observer.assertThatStartedAt(1);
        observer.assertThatFailedAt(2);
    }
    
    private LogMessage logMessage() {
        return LogMessage.builder()
                .type(LogMessage.Level.DEBUG)
                .pattern("someCode")
                .text("someText")
                .build();
    }
    
    private WritableAudioData writableAudioData() {
        return WritableAudioData.builder().build();
    }

    private static class ScanJobServiceObserver implements ScanJobService.Observer {

        private final List<ScanJobProgress> calls = new ArrayList<>();
        
        public int getCallCount() {
            return calls.size();
        }
        
        public void assertThatStartingAt(int index) {
            assertThat(calls).element(index).satisfies(scanJobProgress -> {
                assertThat(scanJobProgress.getScanJob().getStatus()).isEqualTo(STARTING);
                assertThat(scanJobProgress.getScanProgress()).isNull();
            });
        }
        
        public void assertThatStartedAt(int index) {
            assertThat(calls).element(index).satisfies(scanJobProgress -> {
                assertThat(scanJobProgress.getScanJob().getStatus()).isEqualTo(STARTED);
                assertThat(scanJobProgress.getScanProgress()).isNull();
            });
        }
        
        public void assertThatProgressedAt(int index, Consumer<ScanJobProgress> handler) {
            assertThat(calls).element(index).satisfies(scanJobProgress -> {
                assertThat(scanJobProgress.getScanJob().getStatus()).isEqualTo(STARTED);
                assertThat(scanJobProgress.getScanProgress()).isNotNull();
                handler.accept(scanJobProgress);
            });
        }

        public void assertThatCompletedAt(int index) {
            assertThat(calls).element(index).satisfies(scanJobProgress -> {
                assertThat(scanJobProgress.getScanJob().getStatus()).isEqualTo(COMPLETE);
                assertThat(scanJobProgress.getScanProgress()).isNull();
            });
        }

        public void assertThatFailedAt(int index) {
            assertThat(calls).element(index).satisfies(scanJobProgress -> {
                assertThat(scanJobProgress.getScanJob().getStatus()).isEqualTo(FAILED);
                assertThat(scanJobProgress.getScanProgress()).isNull();
            });
        }

        @Override
        public void onScanJobStarting(ScanJob scanJob) {
            assertThat(scanJob).isNotNull();
            assertThat(scanJob.getStatus()).isEqualTo(STARTING);
            calls.add(new ScanJobProgress(scanJob, null));
        }

        @Override
        public void onScanJobStarted(ScanJob scanJob) {
            assertThat(scanJob).isNotNull();
            assertThat(scanJob.getStatus()).isEqualTo(STARTED);
            calls.add(new ScanJobProgress(scanJob, null));
        }

        @Override
        public void onScanJobProgress(ScanJobProgress scanJobProgress) {
            assertThat(scanJobProgress).isNotNull();
            assertThat(scanJobProgress.getScanJob().getStatus()).isEqualTo(STARTED);
            assertThat(scanJobProgress.getScanProgress()).isNotNull();
            calls.add(scanJobProgress);
        }

        @Override
        public void onScanJobCompleted(ScanJob scanJob) {
            assertThat(scanJob).isNotNull();
            assertThat(scanJob.getStatus()).isEqualTo(COMPLETE);
            calls.add(new ScanJobProgress(scanJob, null));
        }

        @Override
        public void onScanJobFailed(ScanJob scanJob) {
            assertThat(scanJob).isNotNull();
            assertThat(scanJob.getStatus()).isEqualTo(FAILED);
            calls.add(new ScanJobProgress(scanJob, null));
        }
    }
    
    private static class ThrowingScanJobServiceObserver implements ScanJobService.Observer {

        private int callCount = 0;

        public int getCallCount() {
            return callCount;
        }

        @Override
        public void onScanJobStarting(ScanJob scanJob) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onScanJobStarted(ScanJob scanJob) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onScanJobProgress(ScanJobProgress scanJobProgress) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onScanJobCompleted(ScanJob scanJob) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onScanJobFailed(ScanJob scanJob) {
            callCount++;
            throw new RuntimeException();
        }
    }
}
