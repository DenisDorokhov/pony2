package net.dorokhov.pony2.core.library.service;

import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.domain.ScanProgress.Value;
import net.dorokhov.pony2.api.library.service.ScanJobService;
import net.dorokhov.pony2.api.library.service.command.EditCommand;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.repository.ScanJobRepository;
import net.dorokhov.pony2.core.library.service.scan.LibraryScanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static net.dorokhov.pony2.api.library.domain.ScanJob.Status.*;
import static net.dorokhov.pony2.api.library.domain.ScanProgress.Step.EDIT_PREPARING;
import static net.dorokhov.pony2.api.library.domain.ScanProgress.Step.FULL_PREPARING;
import static net.dorokhov.pony2.api.library.domain.ScanType.FULL;
import static net.dorokhov.pony2.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static net.dorokhov.pony2.test.ScanJobFixtures.scanJobEdit;
import static net.dorokhov.pony2.test.ScanJobFixtures.scanJobFull;
import static net.dorokhov.pony2.test.ScanResultFixtures.scanResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.transaction.support.TransactionSynchronizationManager.*;

@ExtendWith(MockitoExtension.class)
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

    @BeforeEach
    public void setUp() {
        initSynchronization();
    }

    @AfterEach
    public void tearDown() {
        clearSynchronization();
    }

    @Test
    public void shouldGetAll() {

        Page<ScanJob> page = new PageImpl<>(emptyList());
        when(scanJobRepository.findAll((Pageable) any())).thenReturn(page);

        assertThat(scanJobService.getAll(PageRequest.of(0, 10))).isSameAs(page);
    }

    @Test
    public void shouldGetById() {

        ScanJob scanJob = scanJobFull();
        when(scanJobRepository.findById(any())).thenReturn(Optional.of(scanJob));

        assertThat(scanJobService.getById("1")).containsSame(scanJob);
    }

    @Test
    public void shouldGetLastSuccessfulJob() {

        ScanJob scanJob = scanJobFull();
        when(scanJobRepository.findFirstByStatusOrderByUpdateDateDesc(any())).thenReturn(Optional.of(scanJob));

        assertThat(scanJobService.getLastSuccessfulJob()).containsSame(scanJob);
    }

    @Test
    public void shouldExecuteScanJob() throws IOException, ConcurrentScanException {

        when(configService.getLibraryFolders()).thenReturn(ImmutableList.of(new File("someFolder")));
        when(logService.info(any(), any(), any())).thenReturn(logMessage());
        when(scanJobRepository.save(any())).then(returnsFirstArg());
        ScanResult scanResult = scanResult(FULL);
        when(libraryScanner.scan(any(), any())).then(invocation -> {
            Consumer<ScanProgress> observer = invocation.getArgument(1);
            observer.accept(new ScanProgress(FULL_PREPARING, emptyList(), Value.of(1, 1)));
            return scanResult;
        });

        ScanJobServiceObserver observer = new ScanJobServiceObserver();
        scanJobService.addObserver(observer);

        ScanJob scanJobStarting = scanJobService.startScanJob();
        assertThat(scanJobStarting.getScanType()).isSameAs(FULL);
        assertThat(scanJobStarting.getStatus()).isSameAs(STARTING);
        assertThat(scanJobStarting.getLogMessage()).isNotNull();
        assertThat(scanJobStarting.getScanResult()).isNull();
        verify(logService).info(any(), any(), any());

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());
        verify(logService, times(3)).info(any(), any(), any());

        ScanJob scanJobComplete = savedScanJob.getAllValues().get(2);

        assertThat(scanJobComplete.getScanResult()).isSameAs(scanResult);

        assertThat(observer.getCallCount()).isEqualTo(5);

        observer.assertThatStartingAt(0);
        observer.assertThatStartedAt(1);
        observer.assertThatProgressedAt(2, scanJobProgress -> {
            //noinspection ConstantConditions
            assertThat(scanJobProgress.getScanProgress().getStep()).isSameAs(FULL_PREPARING);
            assertThat(scanJobProgress.getScanProgress().getFiles()).isEmpty();
            assertThat(scanJobProgress.getScanProgress().getValue()).isEqualTo(Value.of(1, 1));
        });

        observer.assertThatCompletingAt(3);
        observer.assertThatCompletedAt(4);

        scanJobService.removeObserver(observer);

        scanJobService.startScanJob();

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(observer.getCallCount()).isEqualTo(5);
    }

    @Test
    public void shouldExecuteEditJob() throws IOException, ConcurrentScanException {

        when(logService.info(any(), any(), any())).thenReturn(logMessage());
        when(scanJobRepository.save(any())).then(returnsFirstArg());
        ScanResult scanResult = scanResult(ScanType.EDIT);
        when(libraryScanner.edit(any(), any(), any())).then(invocation -> {
            Consumer<ScanProgress> observer = invocation.getArgument(2);
            observer.accept(new ScanProgress(EDIT_PREPARING, emptyList(), Value.of(1, 1)));
            return scanResult;
        });

        ScanJobServiceObserver observer = new ScanJobServiceObserver();
        scanJobService.addObserver(observer);

        ScanJob scanJobStarting = scanJobService.startEditJob(ImmutableList.of(new EditCommand("1", new WritableAudioData())));
        assertThat(scanJobStarting.getScanType()).isSameAs(ScanType.EDIT);
        assertThat(scanJobStarting.getStatus()).isSameAs(STARTING);
        assertThat(scanJobStarting.getLogMessage()).isNotNull();
        assertThat(scanJobStarting.getScanResult()).isNull();
        verify(logService).info(any(), any(), any());

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());
        verify(logService, times(3)).info(any(), any(), any());

        ScanJob scanJobComplete = savedScanJob.getAllValues().get(2);

        assertThat(scanJobComplete.getScanResult()).isSameAs(scanResult);

        assertThat(observer.getCallCount()).isEqualTo(5);

        observer.assertThatStartingAt(0);
        observer.assertThatStartedAt(1);
        observer.assertThatProgressedAt(2, scanJobProgress -> {
            //noinspection ConstantConditions
            assertThat(scanJobProgress.getScanProgress().getStep()).isSameAs(EDIT_PREPARING);
            assertThat(scanJobProgress.getScanProgress().getFiles()).isEmpty();
            assertThat(scanJobProgress.getScanProgress().getValue()).isEqualTo(Value.of(1, 1));
        });

        observer.assertThatCompletingAt(3);
        observer.assertThatCompletedAt(4);

        scanJobService.removeObserver(observer);

        scanJobService.startScanJob();

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(observer.getCallCount()).isEqualTo(5);
    }

    @Test
    public void shouldFailScanJobOnIOException() throws IOException, ConcurrentScanException {

        when(logService.info(any(), any(), any())).thenReturn(logMessage());
        
        doTestFailScanJobOnException(new IOException());
        
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldFailScanJobOnConcurrentScanException() throws ConcurrentScanException, InterruptedException {

        when(scanJobRepository.save(any())).then(returnsFirstArg());
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
    public void shouldFailScanJobOnUnexpectedException() throws IOException, ConcurrentScanException {

        when(logService.info(any(), any(), any())).thenReturn(logMessage());

        doTestFailScanJobOnException(new RuntimeException());

        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldFailEditJobOnIOException() throws ConcurrentScanException, IOException {

        when(logService.info(any(), any(), any())).thenReturn(logMessage());

        doTestFailEditJobOnException(new IOException());

        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldFailEditJobOnConcurrentScanException() throws ConcurrentScanException, InterruptedException {

        when(scanJobRepository.save(any())).then(returnsFirstArg());
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
    public void shouldFailEditJobOnUnexpectedException() throws ConcurrentScanException, IOException {

        when(logService.info(any(), any(), any())).thenReturn(logMessage());

        doTestFailEditJobOnException(new RuntimeException());

        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldIgnoreExceptionsThrownByObservers() throws ConcurrentScanException {

        when(scanJobRepository.save(any())).then(returnsFirstArg());
        ThrowingScanJobServiceObserver observer = new ThrowingScanJobServiceObserver();
        scanJobService.addObserver(observer);

        scanJobService.startScanJob();

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(observer.getCallCount()).isEqualTo(4);
    }

    private void doTestFailScanJobOnException(Exception e) throws IOException, ConcurrentScanException {

        when(configService.getLibraryFolders()).thenReturn(ImmutableList.of(new File("someFolder")));
        when(logService.error(any(), any(), any())).thenReturn(logMessage());
        when(scanJobRepository.save(any())).then(returnsFirstArg());
        lenient().when(scanJobRepository.findById(any())).thenReturn(Optional.of(scanJobFull()));
        when(libraryScanner.scan(any(), any())).thenThrow(e);

        ScanJobServiceObserver observer = new ScanJobServiceObserver();
        scanJobService.addObserver(observer);

        scanJobService.startScanJob();
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());
        verify(logService, times(2)).info(any(), any(), any());

        ScanJob scanJobFailed = savedScanJob.getValue();
        assertThat(scanJobFailed.getScanType()).isSameAs(FULL);
        assertThat(scanJobFailed.getStatus()).isSameAs(ScanJob.Status.FAILED);
        assertThat(scanJobFailed.getLogMessage()).isNotNull();
        assertThat(scanJobFailed.getScanResult()).isNull();

        assertThat(observer.getCallCount()).isEqualTo(4);

        observer.assertThatStartingAt(0);
        observer.assertThatStartedAt(1);
        observer.assertThatFailingAt(2);
        observer.assertThatFailedAt(3);
    }

    private void doTestFailEditJobOnException(Exception e) throws IOException, ConcurrentScanException {

        when(logService.error(any(), any(), any())).thenReturn(logMessage());
        when(scanJobRepository.save(any())).then(returnsFirstArg());
        lenient().when(scanJobRepository.findById(any())).thenReturn(Optional.of(scanJobEdit()));
        when(libraryScanner.edit(any(), any(), any())).thenThrow(e);

        ScanJobServiceObserver observer = new ScanJobServiceObserver();
        scanJobService.addObserver(observer);

        scanJobService.startEditJob(ImmutableList.of(new EditCommand("1", new WritableAudioData())));

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());
        verify(logService, times(2)).info(any(), any(), any());

        ScanJob scanJobFailed = savedScanJob.getValue();
        assertThat(scanJobFailed.getScanType()).isSameAs(ScanType.EDIT);
        assertThat(scanJobFailed.getStatus()).isSameAs(ScanJob.Status.FAILED);
        assertThat(scanJobFailed.getLogMessage()).isNotNull();
        assertThat(scanJobFailed.getScanResult()).isNull();

        assertThat(observer.getCallCount()).isEqualTo(4);

        observer.assertThatStartingAt(0);
        observer.assertThatStartedAt(1);
        observer.assertThatFailingAt(2);
        observer.assertThatFailedAt(3);
    }

    private Optional<LogMessage> logMessage() {
        return Optional.of(new LogMessage()
                .setLevel(LogMessage.Level.INFO)
                .setPattern("someCode")
                .setText("someText"));
    }

    private static class ScanJobServiceObserver implements ScanJobService.Observer {

        private static class Call {

            private enum Type {
                STARTING, STARTED, PROGRESS, COMPLETING, COMPLETED, FAILING, FAILED
            }

            private final Type type;
            private final ScanJobProgress scanJobProgress;

            public Call(Type type) {
                this(type, null);
            }

            public Call(Type type, @Nullable ScanJobProgress scanJobProgress) {
                this.type = checkNotNull(type);
                this.scanJobProgress = scanJobProgress;
            }

            public Type getType() {
                return type;
            }

            @Nullable
            public ScanJobProgress getScanJobProgress() {
                return scanJobProgress;
            }
        }

        private final List<Call> calls = new ArrayList<>();

        public int getCallCount() {
            return calls.size();
        }

        public void assertThatStartingAt(int index) {
            assertThat(calls).element(index).satisfies(call ->
                    assertThat(call.getType()).isSameAs(Call.Type.STARTING));
        }

        public void assertThatStartedAt(int index) {
            assertThat(calls).element(index).satisfies(call ->
                    assertThat(call.getType()).isSameAs(Call.Type.STARTED));
        }

        public void assertThatProgressedAt(int index, Consumer<ScanJobProgress> handler) {
            assertThat(calls).element(index).satisfies(call -> {
                assertThat(call.getType()).isSameAs(Call.Type.PROGRESS);
                handler.accept(call.getScanJobProgress());
            });
        }

        public void assertThatCompletingAt(int index) {
            assertThat(calls).element(index).satisfies(call ->
                    assertThat(call.getType()).isSameAs(Call.Type.COMPLETING));
        }

        public void assertThatCompletedAt(int index) {
            assertThat(calls).element(index).satisfies(call ->
                    assertThat(call.getType()).isSameAs(Call.Type.COMPLETED));
        }

        public void assertThatFailingAt(int index) {
            assertThat(calls).element(index).satisfies(call ->
                    assertThat(call.getType()).isSameAs(Call.Type.FAILING));
        }

        public void assertThatFailedAt(int index) {
            assertThat(calls).element(index).satisfies(call ->
                    assertThat(call.getType()).isSameAs(Call.Type.FAILED));
        }

        @Override
        public void onScanJobStarting(ScanJob scanJob) {
            assertThat(scanJob).isNotNull();
            assertThat(scanJob.getStatus()).isSameAs(STARTING);
            calls.add(new Call(Call.Type.STARTING));
        }

        @Override
        public void onScanJobStarted(ScanJob scanJob) {
            assertThat(scanJob).isNotNull();
            assertThat(scanJob.getStatus()).isSameAs(STARTED);
            assertThat(scanJob.getLogMessage()).isNotNull();
            assertThat(scanJob.getScanResult()).isNull();
            calls.add(new Call(Call.Type.STARTED));
        }

        @Override
        public void onScanJobProgress(ScanJobProgress scanJobProgress) {
            assertThat(scanJobProgress).isNotNull();
            assertThat(scanJobProgress.getScanJob().getStatus()).isSameAs(STARTED);
            assertThat(scanJobProgress.getScanProgress()).isNotNull();
            calls.add(new Call(Call.Type.PROGRESS, scanJobProgress));
        }

        @Override
        public void onScanJobCompleting(ScanJob scanJob) {
            assertThat(scanJob).isNotNull();
            assertThat(scanJob.getStatus()).isSameAs(STARTED);
            calls.add(new Call(Call.Type.COMPLETING));
        }

        @Override
        public void onScanJobCompleted(ScanJob scanJob) {
            assertThat(scanJob).isNotNull();
            assertThat(scanJob.getStatus()).isSameAs(COMPLETE);
            assertThat(scanJob.getLogMessage()).isNotNull();
            calls.add(new Call(Call.Type.COMPLETED));
        }

        @Override
        public void onScanJobFailing(ScanJob scanJob) {
            assertThat(scanJob).isNotNull();
            assertThat(scanJob.getStatus()).isSameAs(STARTED);
            calls.add(new Call(Call.Type.FAILING));
        }

        @Override
        public void onScanJobFailed(ScanJob scanJob) {
            assertThat(scanJob).isNotNull();
            assertThat(scanJob.getStatus()).isSameAs(FAILED);
            calls.add(new Call(Call.Type.FAILED));
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
        public void onScanJobCompleting(ScanJob scanJob) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onScanJobCompleted(ScanJob scanJob) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onScanJobFailing(ScanJob scanJob) {
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
