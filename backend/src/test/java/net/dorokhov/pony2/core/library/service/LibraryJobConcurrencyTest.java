package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;
import net.dorokhov.pony2.api.library.service.ScanJobService;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentDiscoveryException;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.DiscoveryCancellationMonitor;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.core.library.service.discovery.AlbumDiscoveryService;
import net.dorokhov.pony2.core.library.service.discovery.ArtistDiscoveryService;
import net.dorokhov.pony2.core.library.service.discovery.DiscoveryScanJobObserver;
import net.dorokhov.pony2.core.library.service.discovery.FullDiscoveryService;
import net.dorokhov.pony2.core.library.service.exception.ScanInterruptedException;
import net.dorokhov.pony2.core.library.service.scan.LibraryScanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static net.dorokhov.pony2.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED;
import static org.springframework.transaction.support.TransactionSynchronizationManager.*;

@ExtendWith(MockitoExtension.class)
class LibraryJobConcurrencyTest {

    private enum JobKind { SCAN, EDIT, DISCOVERY_FULL, DISCOVERY_ARTIST, DISCOVERY_ALBUM }

    @Mock private ScanJobRepository scanJobRepository;
    @Mock private DiscoveryJobRepository discoveryJobRepository;
    @Mock private DiscoveryTaskRepository discoveryTaskRepository;
    @Mock private ConfigService configService;
    @Mock private LibraryScanner libraryScanner;
    @Mock private ArtistRepository artistRepository;
    @Mock private AlbumRepository albumRepository;
    @Mock private FullDiscoveryService fullDiscoveryService;
    @Mock private ArtistDiscoveryService artistDiscoveryService;
    @Mock private AlbumDiscoveryService albumDiscoveryService;
    @Mock private LogService logService;

    private final DiscoveryCancellationMonitor cancellationMonitor = new DiscoveryCancellationMonitor();
    private final LibraryJobLockService lockService = new LibraryJobLockService();
    private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    private final Executor executor = tasks::add;
    private ScanJobServiceImpl scanService;
    private DiscoveryJobServiceImpl discoveryService;

    @BeforeEach
    void setUp() throws Exception {
        initSynchronization();
        scanService = new ScanJobServiceImpl(scanJobRepository, configService, libraryScanner, lockService, cancellationMonitor,
                logService, executor, transactionManager());
        discoveryService = new DiscoveryJobServiceImpl(discoveryJobRepository, discoveryTaskRepository,
                artistRepository, albumRepository, fullDiscoveryService, lockService,
                artistDiscoveryService, albumDiscoveryService, logService, executor, transactionManager());

        lenient().when(configService.get()).thenReturn(new ConfigSet(null, List.of(), null, null, null));
        AtomicInteger ids = new AtomicInteger();
        lenient().when(scanJobRepository.save(any())).thenAnswer(invocation -> {
            ScanJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId("scan-" + ids.incrementAndGet());
            }
            return job;
        });
        lenient().when(discoveryJobRepository.save(any())).thenAnswer(invocation -> {
            DiscoveryJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId("discovery-" + ids.incrementAndGet());
            }
            return job;
        });
        lenient().when(libraryScanner.scan(any(), any())).thenReturn(new ScanResult());
        lenient().when(libraryScanner.edit(any(), any(), any())).thenReturn(new ScanResult());
        lenient().when(artistRepository.findById(any())).thenReturn(Optional.of(new Artist()));
        lenient().when(albumRepository.findById(any())).thenReturn(Optional.of(new Album()));
    }

    @AfterEach
    void tearDown() {
        if (isSynchronizationActive()) {
            clearSynchronization();
        }
    }

    @ParameterizedTest
    @EnumSource(value = JobKind.class, names = {"SCAN", "EDIT"})
    void shouldRejectDiscoveryWhileScanIsQueuedAndRunning(JobKind kind) throws Exception {
        ScanJobService.Observer observer = mock(ScanJobService.Observer.class);
        doAnswer(invocation -> { assertDiscoveryRejected(); return null; }).when(observer).onScanJobStarted(any());
        doAnswer(invocation -> { assertDiscoveryRejected(); return null; }).when(observer).onScanJobCompleting(any());
        scanService.addObserver(observer);

        start(kind);
        commit();
        assertDiscoveryRejected();
        assertThat(tasks).hasSize(1);
        runNextTask();

        verify(observer).onScanJobStarted(any());
        verify(observer).onScanJobCompleting(any());
        assertReleased();
    }

    @ParameterizedTest
    @EnumSource(value = JobKind.class, names = {"SCAN", "EDIT", "DISCOVERY_FULL"})
    void shouldAcquireBeforeSavingJob(JobKind kind) throws Exception {
        if (isScan(kind)) {
            doAnswer(invocation -> {
                assertDiscoveryRejected();
                return invocation.getArgument(0);
            }).when(scanJobRepository).save(any());
        } else {
            doAnswer(invocation -> {
                assertThat(lockService.tryAcquire()).isEmpty();
                return invocation.getArgument(0);
            }).when(discoveryJobRepository).save(any());
        }
        start(kind);
        assertThat(tasks).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = JobKind.class, names = {"SCAN", "EDIT", "DISCOVERY_FULL"})
    void shouldReleaseWhenSavingJobFails(JobKind kind) {
        RuntimeException failure = new IllegalStateException("save failed");
        if (isScan(kind)) {
            doThrow(failure).when(scanJobRepository).save(any());
        } else {
            doThrow(failure).when(discoveryJobRepository).save(any());
        }
        assertThatThrownBy(() -> start(kind)).isSameAs(failure);
        assertThat(tasks).isEmpty();
        assertReleased();
    }

    @ParameterizedTest
    @EnumSource(value = JobKind.class, names = {"SCAN", "EDIT", "DISCOVERY_FULL"})
    void shouldReleaseEvenIfFailureStatusCannotBeSaved(JobKind kind) throws Exception {
        Object job = start(kind);
        RuntimeException failure = new IllegalStateException("database unavailable");
        if (job instanceof ScanJob scanJob) {
            when(scanJobRepository.findById(any())).thenReturn(Optional.of(scanJob));
            doThrow(failure).when(scanJobRepository).save(any());
        } else {
            when(discoveryJobRepository.findById(any())).thenReturn(Optional.of((DiscoveryJob) job));
            doThrow(failure).when(discoveryJobRepository).save(any());
        }
        commit();
        assertThatThrownBy(this::runNextTask).isSameAs(failure);
        assertReleased();
    }

    @Test
    void shouldReleaseAfterScanInterruption() throws Exception {
        ScanJob job = scanService.startScanJob();
        when(scanJobRepository.findById(any())).thenReturn(Optional.of(job));
        when(libraryScanner.scan(any(), any())).thenThrow(new ScanInterruptedException());
        commit();
        runNextTask();
        assertThat(job.getStatus()).isEqualTo(ScanJob.Status.INTERRUPTED);
        assertReleased();
    }

    @Test
    void shouldStartDiscoveryAfterScanReleasesLock() throws Exception {
        scanService.addObserver(new DiscoveryScanJobObserver(scanService, discoveryService, logService));
        scanService.startScanJob();
        commit();
        runNextTask();
        assertThat(scanService.getCurrentScanJobProgress()).isEmpty();
        assertThat(discoveryService.getCurrentDiscoveryJobProgress()).hasValueSatisfying(progress ->
                assertThat(progress.getDiscoveryJob().getStatus()).isEqualTo(DiscoveryJob.Status.STARTING));
        assertThat(tasks).hasSize(1);
        assertThat(lockService.tryAcquire()).isEmpty();
        runNextTask();
        verify(fullDiscoveryService).discover(any(), eq(true), any());
        assertReleased();
    }

    @Test
    void shouldPreserveProgressOfJobStartedByCompletionObserver() throws Exception {
        ScanJobService.Observer observer = mock(ScanJobService.Observer.class);
        doAnswer(invocation -> {
            assertThat(scanService.getCurrentScanJobProgress()).isEmpty();
            scanService.startEditJob(List.of());
            commit();
            return null;
        }).when(observer).onScanJobCompleted(any());
        scanService.addObserver(observer);
        scanService.startScanJob();
        commit();
        runNextTask();
        verify(observer).onScanJobCompleted(any());
        assertThat(scanService.getCurrentScanJobProgress()).hasValueSatisfying(progress -> {
            assertThat(progress.getScanJob().getScanType()).isEqualTo(ScanType.EDIT);
            assertThat(progress.getScanJob().getStatus()).isEqualTo(ScanJob.Status.STARTING);
        });
        assertDiscoveryRejected();
    }

    @Test
    void shouldAllowOnlyOneSimultaneousScanStart() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try (ExecutorService callers = Executors.newFixedThreadPool(2)) {
            Future<Object> scan = callers.submit(() -> concurrentStart(JobKind.SCAN, ready, go));
            Future<Object> edit = callers.submit(() -> concurrentStart(JobKind.EDIT, ready, go));
            try {
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                go.countDown();
            }
            List<Object> results = List.of(scan.get(5, TimeUnit.SECONDS), edit.get(5, TimeUnit.SECONDS));
            assertThat(results.stream().filter(ConcurrentScanException.class::isInstance).count()).isEqualTo(1);
            long saves = mockingDetails(scanJobRepository).getInvocations().size()
                    + mockingDetails(discoveryJobRepository).getInvocations().size();
            assertThat(saves).isEqualTo(1);
            assertThat(tasks).isEmpty();
        }
    }

    @ParameterizedTest
    @EnumSource(value = JobKind.class, names = {"DISCOVERY_FULL", "DISCOVERY_ARTIST", "DISCOVERY_ALBUM"})
    void shouldRejectScanWhileDiscoveryIsQueued(JobKind kind) throws Exception {
        start(kind);
        commit();

        assertThat(cancellationMonitor.hasRunningTasks()).isFalse();
        assertThatThrownBy(scanService::startScanJob).isInstanceOf(ConcurrentScanException.class);
        assertThatThrownBy(() -> scanService.startEditJob(List.of())).isInstanceOf(ConcurrentScanException.class);
        verifyNoInteractions(scanJobRepository);
        assertThat(tasks).hasSize(1);
        runNextTask();
        assertReleased();
    }

    @ParameterizedTest
    @EnumSource(value = JobKind.class, names = {"SCAN", "EDIT"})
    void shouldKeepScanBlockedAfterTaskCancellationUntilDiscoveryJobFinishes(JobKind kind) throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finishing = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        doAnswer(invocation -> {
            cancellationMonitor.taskStarted();
            try {
                entered.countDown();
                assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
                cancellationMonitor.interruptIfCancelled();
                return null;
            } finally {
                cancellationMonitor.taskFinished();
            }
        }).when(fullDiscoveryService).discover(any(), eq(true), any());
        DiscoveryJob discovery = discoveryService.startFullJob();
        when(discoveryJobRepository.findById(any())).thenReturn(Optional.of(discovery));
        DiscoveryJobService.Observer observer = mock(DiscoveryJobService.Observer.class);
        doAnswer(invocation -> {
            finishing.countDown();
            assertThat(finish.await(5, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(observer).onDiscoveryJobInterrupting(any());
        discoveryService.addObserver(observer);
        commit();

        try (ExecutorService callers = Executors.newFixedThreadPool(2)) {
            Future<?> execution = callers.submit(tasks.remove());
            Future<Object> scan = null;
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                scan = callers.submit(() -> startAndCommit(kind));
                await().atMost(Duration.ofSeconds(5)).until(cancellationMonitor::isCancelled);
                assertThat(scan.isDone()).isFalse();
                verifyNoInteractions(scanJobRepository);
                release.countDown();
                assertThat(finishing.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(cancellationMonitor.hasRunningTasks()).isFalse();
                assertThat(scan.get(5, TimeUnit.SECONDS)).isInstanceOf(ConcurrentScanException.class);
                verifyNoInteractions(scanJobRepository);
                finish.countDown();
                execution.get(5, TimeUnit.SECONDS);
                verify(observer).onDiscoveryJobInterrupted(any());
                assertThat(discovery.getStatus()).isEqualTo(DiscoveryJob.Status.INTERRUPTED);
                assertReleased();

                start(kind);
                commit();
                runNextTask();
                assertReleased();
            } finally {
                release.countDown();
                finish.countDown();
                if (scan != null) {
                    scan.cancel(true);
                }
            }
        }
    }

    private Object startAndCommit(JobKind kind) throws Exception {
        initSynchronization();
        try {
            Object job = start(kind);
            commit();
            return job;
        } catch (ConcurrentScanException e) {
            return e;
        } finally {
            clearSynchronization();
        }
    }

    private Object concurrentStart(JobKind kind, CountDownLatch ready, CountDownLatch go) throws Exception {
        initSynchronization();
        try {
            ready.countDown();
            assertThat(go.await(5, TimeUnit.SECONDS)).isTrue();
            return start(kind);
        } catch (ConcurrentScanException | ConcurrentDiscoveryException e) {
            return e;
        } finally {
            clearSynchronization();
        }
    }

    private Object start(JobKind kind) throws ConcurrentScanException, ConcurrentDiscoveryException {
        return switch (kind) {
            case SCAN -> scanService.startScanJob();
            case EDIT -> scanService.startEditJob(List.of());
            case DISCOVERY_FULL -> discoveryService.startFullJob();
            case DISCOVERY_ARTIST -> discoveryService.startArtistJob("artist");
            case DISCOVERY_ALBUM -> discoveryService.startAlbumJob("album");
        };
    }

    private boolean isScan(JobKind kind) {
        return kind == JobKind.SCAN || kind == JobKind.EDIT;
    }

    private void assertDiscoveryRejected() {
        int scanSaves = mockingDetails(scanJobRepository).getInvocations().size();
        int discoverySaves = mockingDetails(discoveryJobRepository).getInvocations().size();
        int logs = mockingDetails(logService).getInvocations().size();
        for (JobKind kind : JobKind.values()) {
            if (isScan(kind)) {
                continue;
            }
            assertThatThrownBy(() -> start(kind))
                    .isInstanceOf(ConcurrentDiscoveryException.class);
        }
        assertThat(mockingDetails(scanJobRepository).getInvocations()).hasSize(scanSaves);
        assertThat(mockingDetails(discoveryJobRepository).getInvocations()).hasSize(discoverySaves);
        assertThat(mockingDetails(logService).getInvocations()).hasSize(logs);
    }

    private void assertReleased() {
        assertThat(cancellationMonitor.hasRunningTasks()).isFalse();
        assertThat(scanService.getCurrentScanJobProgress()).isEmpty();
        assertThat(discoveryService.getCurrentDiscoveryJobProgress()).isEmpty();
        try (LibraryJobLockService.Permit ignored = lockService.tryAcquire().orElseThrow()) {
            assertThat(lockService.tryAcquire()).isEmpty();
        }
    }

    private void commit() {
        List<TransactionSynchronization> synchronizations = getSynchronizations();
        try {
            synchronizations.forEach(TransactionSynchronization::afterCommit);
        } finally {
            synchronizations.forEach(synchronization -> synchronization.afterCompletion(STATUS_COMMITTED));
            clearSynchronization();
            initSynchronization();
        }
    }

    private void runNextTask() {
        Runnable task = tasks.poll();
        assertThat(task).isNotNull();
        task.run();
        commit();
    }
}
