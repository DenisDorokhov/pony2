package net.dorokhov.pony2.core.library.service;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentDiscoveryException;
import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.core.library.service.discovery.AlbumDiscoveryService;
import net.dorokhov.pony2.core.library.service.discovery.ArtistDiscoveryService;
import net.dorokhov.pony2.core.library.service.discovery.FullDiscoveryService;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static net.dorokhov.pony2.api.library.domain.DiscoveryJob.Status.*;
import static net.dorokhov.pony2.api.library.domain.DiscoveryProgress.Step.*;
import static net.dorokhov.pony2.api.library.domain.DiscoveryType.*;
import static net.dorokhov.pony2.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static net.dorokhov.pony2.test.DiscoveryJobFixtures.discoveryJobArtist;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.transaction.support.TransactionSynchronizationManager.*;

@ExtendWith(MockitoExtension.class)
public class DiscoveryJobServiceImplTest {

    @InjectMocks
    private DiscoveryJobServiceImpl discoveryJobService;

    @Mock
    private DiscoveryJobRepository discoveryJobRepository;
    @Mock
    private DiscoveryTaskRepository discoveryTaskRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private FullDiscoveryService fullDiscoveryService;
    @Mock
    private ArtistDiscoveryService artistDiscoveryService;
    @Mock
    private AlbumDiscoveryService albumDiscoveryService;
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
        lenient().when(discoveryTaskRepository.countByJobId(any())).thenReturn(0L);
        lenient().when(discoveryTaskRepository.countByJobIdAndStatus(any(), any())).thenReturn(0L);
    }

    @AfterEach
    public void tearDown() {
        clearSynchronization();
    }

    @Test
    public void shouldGetAll() {

        Page<DiscoveryJob> page = new PageImpl<>(emptyList());
        when(discoveryJobRepository.findAll((Pageable) any())).thenReturn(page);

        assertThat(discoveryJobService.getAll(PageRequest.of(0, 10))).isSameAs(page);
    }

    @Test
    public void shouldGetById() {

        DiscoveryJob discoveryJob = discoveryJobArtist();
        when(discoveryJobRepository.findById(any())).thenReturn(Optional.of(discoveryJob));

        assertThat(discoveryJobService.getById("1")).containsSame(discoveryJob);
    }

    @Test
    public void shouldExecuteFullJob() throws ConcurrentDiscoveryException {

        when(logService.info(any(), any(), any())).thenReturn(logMessage());
        when(discoveryJobRepository.save(any())).then(saveDiscoveryJob());
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(1);
            observer.accept(new DiscoveryProgress(FULL_ARTIST_DISCOVERY, null));
            return null;
        }).when(fullDiscoveryService).discover(any(), any());

        DiscoveryJobServiceObserver observer = new DiscoveryJobServiceObserver();
        discoveryJobService.addObserver(observer);

        DiscoveryJob discoveryJobStarting = discoveryJobService.startFullJob();
        assertThat(discoveryJobStarting.getType()).isSameAs(FULL);
        assertThat(discoveryJobStarting.getStatus()).isSameAs(STARTING);
        assertThat(discoveryJobStarting.getParameter()).isNull();
        assertThat(discoveryJobStarting.getLogMessage()).isNotNull();
        assertThat(discoveryJobStarting.getDiscoveryResult()).isNull();

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<DiscoveryJob> savedDiscoveryJob = ArgumentCaptor.forClass(DiscoveryJob.class);
        verify(discoveryJobRepository, times(3)).save(savedDiscoveryJob.capture());
        verify(logService, times(3)).info(any(), any(), any());
        verify(fullDiscoveryService).discover(any(), any());

        DiscoveryJob discoveryJobComplete = savedDiscoveryJob.getValue();
        assertThat(discoveryJobComplete.getStatus()).isSameAs(COMPLETE);
        assertThat(discoveryJobComplete.getDiscoveryResult()).isNotNull();
        assertThat(discoveryJobComplete.getDiscoveryResult().getCompletedTasks()).isZero();
        assertThat(discoveryJobComplete.getDiscoveryResult().getFailedTasks()).isZero();

        assertThat(observer.getCallCount()).isEqualTo(5);
        observer.assertThatStartingAt(0);
        observer.assertThatStartedAt(1);
        observer.assertThatProgressedAt(2, discoveryJobProgress -> {
            //noinspection ConstantConditions
            assertThat(discoveryJobProgress.getDiscoveryProgress().getStep()).isSameAs(FULL_ARTIST_DISCOVERY);
            assertThat(discoveryJobProgress.getDiscoveryProgress().getValue()).isNull();
        });
        observer.assertThatCompletingAt(3);
        observer.assertThatCompletedAt(4);

        discoveryJobService.removeObserver(observer);

        discoveryJobService.startFullJob();
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(observer.getCallCount()).isEqualTo(5);
    }

    @Test
    public void shouldExecuteArtistJob() throws ConcurrentDiscoveryException {

        Artist artist = artist("artist1");
        when(artistRepository.findById("artist1")).thenReturn(Optional.of(artist));
        when(logService.info(any(), any(), any())).thenReturn(logMessage());
        when(discoveryJobRepository.save(any())).then(saveDiscoveryJob());
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(2);
            observer.accept(new DiscoveryProgress(ARTIST_DISCOVERY, null));
            return null;
        }).when(artistDiscoveryService).discover(any(), any(), any());

        DiscoveryJobServiceObserver observer = new DiscoveryJobServiceObserver();
        discoveryJobService.addObserver(observer);

        discoveryJobService.startArtistJob("artist1");
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<DiscoveryJob> savedDiscoveryJob = ArgumentCaptor.forClass(DiscoveryJob.class);
        verify(discoveryJobRepository, times(3)).save(savedDiscoveryJob.capture());
        verify(artistDiscoveryService).discover(any(), same(artist), any());

        DiscoveryJob discoveryJobComplete = savedDiscoveryJob.getValue();
        assertThat(discoveryJobComplete.getType()).isSameAs(ARTIST);
        assertThat(discoveryJobComplete.getStatus()).isSameAs(COMPLETE);
        assertThat(discoveryJobComplete.getParameter()).isEqualTo("artist1");
        assertThat(discoveryJobComplete.getDiscoveryResult()).isNotNull();

        assertThat(observer.getCallCount()).isEqualTo(5);
        observer.assertThatStartingAt(0);
        observer.assertThatStartedAt(1);
        observer.assertThatProgressedAt(2, discoveryJobProgress -> {
            //noinspection ConstantConditions
            assertThat(discoveryJobProgress.getDiscoveryProgress().getStep()).isSameAs(ARTIST_DISCOVERY);
            assertThat(discoveryJobProgress.getDiscoveryProgress().getValue()).isNull();
        });
        observer.assertThatCompletingAt(3);
        observer.assertThatCompletedAt(4);
    }

    @Test
    public void shouldExecuteAlbumJob() throws ConcurrentDiscoveryException {

        Album album = album("album1");
        when(albumRepository.findById("album1")).thenReturn(Optional.of(album));
        when(logService.info(any(), any(), any())).thenReturn(logMessage());
        when(discoveryJobRepository.save(any())).then(saveDiscoveryJob());
        doAnswer(invocation -> {
            Consumer<DiscoveryProgress> observer = invocation.getArgument(2);
            observer.accept(new DiscoveryProgress(ALBUM_DISCOVERY, null));
            return null;
        }).when(albumDiscoveryService).discover(any(), any(), any());

        DiscoveryJobServiceObserver observer = new DiscoveryJobServiceObserver();
        discoveryJobService.addObserver(observer);

        discoveryJobService.startAlbumJob("album1");
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<DiscoveryJob> savedDiscoveryJob = ArgumentCaptor.forClass(DiscoveryJob.class);
        verify(discoveryJobRepository, times(3)).save(savedDiscoveryJob.capture());
        verify(albumDiscoveryService).discover(any(), same(album), any());

        DiscoveryJob discoveryJobComplete = savedDiscoveryJob.getValue();
        assertThat(discoveryJobComplete.getType()).isSameAs(ALBUM);
        assertThat(discoveryJobComplete.getStatus()).isSameAs(COMPLETE);
        assertThat(discoveryJobComplete.getParameter()).isEqualTo("album1");
        assertThat(discoveryJobComplete.getDiscoveryResult()).isNotNull();

        assertThat(observer.getCallCount()).isEqualTo(5);
        observer.assertThatStartingAt(0);
        observer.assertThatStartedAt(1);
        observer.assertThatProgressedAt(2, discoveryJobProgress -> {
            //noinspection ConstantConditions
            assertThat(discoveryJobProgress.getDiscoveryProgress().getStep()).isSameAs(ALBUM_DISCOVERY);
            assertThat(discoveryJobProgress.getDiscoveryProgress().getValue()).isNull();
        });
        observer.assertThatCompletingAt(3);
        observer.assertThatCompletedAt(4);
    }

    @Test
    public void shouldModerateDiscoveryJobOnPartiallyFailedTasks() throws ConcurrentDiscoveryException {

        when(artistRepository.findById("artist1")).thenReturn(Optional.of(artist("artist1")));
        when(discoveryTaskRepository.countByJobId(any())).thenReturn(2L);
        when(discoveryTaskRepository.countByJobIdAndStatus(any(), any())).thenReturn(1L);
        when(logService.info(any(), any(), any())).thenReturn(logMessage());
        when(logService.warn(any(), any(), any())).thenReturn(logMessage());
        when(discoveryJobRepository.save(any())).then(saveDiscoveryJob());

        DiscoveryJobServiceObserver observer = new DiscoveryJobServiceObserver();
        discoveryJobService.addObserver(observer);

        discoveryJobService.startArtistJob("artist1");
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<DiscoveryJob> savedDiscoveryJob = ArgumentCaptor.forClass(DiscoveryJob.class);
        verify(discoveryJobRepository, times(3)).save(savedDiscoveryJob.capture());
        verify(logService).warn(any(), any(), any());

        DiscoveryJob discoveryJobModerate = savedDiscoveryJob.getValue();
        assertThat(discoveryJobModerate.getStatus()).isSameAs(MODERATE);
        assertThat(discoveryJobModerate.getDiscoveryResult().getCompletedTasks()).isEqualTo(1L);
        assertThat(discoveryJobModerate.getDiscoveryResult().getFailedTasks()).isEqualTo(1L);

        assertThat(observer.getCallCount()).isEqualTo(4);
        observer.assertThatStartingAt(0);
        observer.assertThatStartedAt(1);
        observer.assertThatModeratingAt(2);
        observer.assertThatModeratedAt(3);
    }

    @Test
    public void shouldFailDiscoveryJobWhenAllTasksFailed() throws ConcurrentDiscoveryException {

        when(artistRepository.findById("artist1")).thenReturn(Optional.of(artist("artist1")));
        when(discoveryTaskRepository.countByJobId(any())).thenReturn(2L);
        when(discoveryTaskRepository.countByJobIdAndStatus(any(), any())).thenReturn(0L);
        when(logService.info(any(), any(), any())).thenReturn(logMessage());
        when(logService.error(any(), any(), any())).thenReturn(logMessage());
        when(discoveryJobRepository.save(any())).then(saveDiscoveryJob());

        DiscoveryJobServiceObserver observer = new DiscoveryJobServiceObserver();
        discoveryJobService.addObserver(observer);

        discoveryJobService.startArtistJob("artist1");
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<DiscoveryJob> savedDiscoveryJob = ArgumentCaptor.forClass(DiscoveryJob.class);
        verify(discoveryJobRepository, times(3)).save(savedDiscoveryJob.capture());
        verify(logService).error(any(), any(), any());

        DiscoveryJob discoveryJobFailed = savedDiscoveryJob.getValue();
        assertThat(discoveryJobFailed.getStatus()).isSameAs(FAILED);
        assertThat(discoveryJobFailed.getDiscoveryResult().getCompletedTasks()).isZero();
        assertThat(discoveryJobFailed.getDiscoveryResult().getFailedTasks()).isEqualTo(2L);

        assertThat(observer.getCallCount()).isEqualTo(4);
        observer.assertThatStartingAt(0);
        observer.assertThatStartedAt(1);
        observer.assertThatFailingAt(2);
        observer.assertThatFailedAt(3);
    }

    @Test
    public void shouldFailDiscoveryJobOnConcurrentDiscoveryException() throws ConcurrentDiscoveryException, InterruptedException {

        when(discoveryJobRepository.save(any())).then(saveDiscoveryJob());
        discoveryJobService.startFullJob();

        assertThatThrownBy(() -> discoveryJobService.startFullJob()).isInstanceOf(ConcurrentDiscoveryException.class);

        AtomicBoolean isConcurrentDiscovery = new AtomicBoolean(false);
        Thread thread = new Thread(() -> {
            try {
                discoveryJobService.startFullJob();
            } catch (ConcurrentDiscoveryException e) {
                isConcurrentDiscovery.set(true);
            }
        });
        thread.start();
        thread.join();
        assertThat(isConcurrentDiscovery.get()).isTrue();
    }

    @Test
    public void shouldFailDiscoveryJobOnUnexpectedException() throws ConcurrentDiscoveryException {

        when(artistRepository.findById("artist1")).thenReturn(Optional.of(artist("artist1")));
        doThrow(new RuntimeException()).when(artistDiscoveryService).discover(any(), any(), any());
        when(logService.info(any(), any(), any())).thenReturn(logMessage());
        when(logService.error(any(), any(), any())).thenReturn(logMessage());
        when(discoveryJobRepository.findById(any())).thenReturn(Optional.of(discoveryJobArtist().setStatus(STARTED)));
        when(discoveryJobRepository.save(any())).then(saveDiscoveryJob());

        DiscoveryJobServiceObserver observer = new DiscoveryJobServiceObserver();
        discoveryJobService.addObserver(observer);

        discoveryJobService.startArtistJob("artist1");
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<DiscoveryJob> savedDiscoveryJob = ArgumentCaptor.forClass(DiscoveryJob.class);
        verify(discoveryJobRepository, times(3)).save(savedDiscoveryJob.capture());
        verify(logService, times(2)).info(any(), any(), any());
        verify(logService).error(any(), any(), any());

        DiscoveryJob discoveryJobFailed = savedDiscoveryJob.getValue();
        assertThat(discoveryJobFailed.getType()).isSameAs(ARTIST);
        assertThat(discoveryJobFailed.getStatus()).isSameAs(FAILED);
        assertThat(discoveryJobFailed.getLogMessage()).isNotNull();
        assertThat(discoveryJobFailed.getDiscoveryResult()).isNull();

        assertThat(observer.getCallCount()).isEqualTo(4);
        observer.assertThatStartingAt(0);
        observer.assertThatStartedAt(1);
        observer.assertThatFailingAt(2);
        observer.assertThatFailedAt(3);
    }

    @Test
    public void shouldInterruptDiscoveryJobOnInterruption() throws ConcurrentDiscoveryException {

        when(artistRepository.findById("artist1")).thenReturn(Optional.of(artist("artist1")));
        doThrow(new RuntimeException(new DiscoveryInterruptedException()))
                .when(artistDiscoveryService).discover(any(), any(), any());
        when(logService.info(any(), any(), any())).thenReturn(logMessage());
        when(logService.warn(any(), any(), any())).thenReturn(logMessage());
        when(discoveryJobRepository.findById(any())).thenReturn(Optional.of(discoveryJobArtist().setStatus(STARTED)));
        when(discoveryJobRepository.save(any())).then(saveDiscoveryJob());

        DiscoveryJobServiceObserver observer = new DiscoveryJobServiceObserver();
        discoveryJobService.addObserver(observer);

        discoveryJobService.startArtistJob("artist1");
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<DiscoveryJob> savedDiscoveryJob = ArgumentCaptor.forClass(DiscoveryJob.class);
        verify(discoveryJobRepository, times(3)).save(savedDiscoveryJob.capture());
        verify(logService).warn(any(), eq("Discovery job has been interrupted."), any());
        verify(logService, never()).error(any(), any(), any());

        DiscoveryJob discoveryJobInterrupted = savedDiscoveryJob.getValue();
        assertThat(discoveryJobInterrupted.getStatus()).isSameAs(INTERRUPTED);
        assertThat(discoveryJobInterrupted.getLogMessage()).isNotNull();
        assertThat(discoveryJobInterrupted.getDiscoveryResult()).isNull();
        assertThat(discoveryJobService.getCurrentDiscoveryJobProgress()).isEmpty();

        assertThat(observer.getCallCount()).isEqualTo(4);
        observer.assertThatStartingAt(0);
        observer.assertThatStartedAt(1);
        observer.assertThatInterruptingAt(2);
        observer.assertThatInterruptedAt(3);
    }

    @Test
    public void shouldIgnoreExceptionsThrownByObservers() throws ConcurrentDiscoveryException {

        when(discoveryJobRepository.save(any())).then(saveDiscoveryJob());

        ThrowingDiscoveryJobServiceObserver observer = new ThrowingDiscoveryJobServiceObserver();
        discoveryJobService.addObserver(observer);

        discoveryJobService.startFullJob();
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(observer.getCallCount()).isEqualTo(4);
    }

    private org.mockito.stubbing.Answer<DiscoveryJob> saveDiscoveryJob() {
        return invocation -> {
            DiscoveryJob discoveryJob = invocation.getArgument(0);
            if (discoveryJob.getId() == null) {
                discoveryJob.setId("1");
            }
            return discoveryJob;
        };
    }

    private Optional<LogMessage> logMessage() {
        return Optional.of(new LogMessage()
                .setLevel(LogMessage.Level.INFO)
                .setPattern("someCode")
                .setText("someText"));
    }

    private Artist artist(String id) {
        return new Artist()
                .setId(id)
                .setName("someArtist");
    }

    private Album album(String id) {
        return new Album()
                .setId(id)
                .setName("someAlbum")
                .setArtist(artist("artist1"));
    }

    private static class DiscoveryJobServiceObserver implements DiscoveryJobService.Observer {

        private static class Call {

            private enum Type {
                STARTING, STARTED, PROGRESS, COMPLETING, COMPLETED, MODERATING, MODERATED, FAILING, FAILED, INTERRUPTING, INTERRUPTED
            }

            private final Type type;
            private final DiscoveryJobProgress discoveryJobProgress;

            public Call(Type type) {
                this(type, null);
            }

            public Call(Type type, @Nullable DiscoveryJobProgress discoveryJobProgress) {
                this.type = checkNotNull(type);
                this.discoveryJobProgress = discoveryJobProgress;
            }

            public Type getType() {
                return type;
            }

            @Nullable
            public DiscoveryJobProgress getDiscoveryJobProgress() {
                return discoveryJobProgress;
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

        public void assertThatProgressedAt(int index, Consumer<DiscoveryJobProgress> handler) {
            assertThat(calls).element(index).satisfies(call -> {
                assertThat(call.getType()).isSameAs(Call.Type.PROGRESS);
                handler.accept(call.getDiscoveryJobProgress());
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

        public void assertThatModeratingAt(int index) {
            assertThat(calls).element(index).satisfies(call ->
                    assertThat(call.getType()).isSameAs(Call.Type.MODERATING));
        }

        public void assertThatModeratedAt(int index) {
            assertThat(calls).element(index).satisfies(call ->
                    assertThat(call.getType()).isSameAs(Call.Type.MODERATED));
        }

        public void assertThatFailingAt(int index) {
            assertThat(calls).element(index).satisfies(call ->
                    assertThat(call.getType()).isSameAs(Call.Type.FAILING));
        }

        public void assertThatFailedAt(int index) {
            assertThat(calls).element(index).satisfies(call ->
                    assertThat(call.getType()).isSameAs(Call.Type.FAILED));
        }

        public void assertThatInterruptingAt(int index) {
            assertThat(calls).element(index).satisfies(call ->
                    assertThat(call.getType()).isSameAs(Call.Type.INTERRUPTING));
        }

        public void assertThatInterruptedAt(int index) {
            assertThat(calls).element(index).satisfies(call ->
                    assertThat(call.getType()).isSameAs(Call.Type.INTERRUPTED));
        }

        @Override
        public void onDiscoveryJobStarting(DiscoveryJob discoveryJob) {
            assertThat(discoveryJob).isNotNull();
            assertThat(discoveryJob.getStatus()).isSameAs(STARTING);
            calls.add(new Call(Call.Type.STARTING));
        }

        @Override
        public void onDiscoveryJobStarted(DiscoveryJob discoveryJob) {
            assertThat(discoveryJob).isNotNull();
            assertThat(discoveryJob.getStatus()).isSameAs(STARTED);
            assertThat(discoveryJob.getLogMessage()).isNotNull();
            assertThat(discoveryJob.getDiscoveryResult()).isNull();
            calls.add(new Call(Call.Type.STARTED));
        }

        @Override
        public void onDiscoveryJobProgress(DiscoveryJobProgress discoveryJobProgress) {
            assertThat(discoveryJobProgress).isNotNull();
            assertThat(discoveryJobProgress.getDiscoveryJob().getStatus()).isSameAs(STARTED);
            assertThat(discoveryJobProgress.getDiscoveryProgress()).isNotNull();
            calls.add(new Call(Call.Type.PROGRESS, discoveryJobProgress));
        }

        @Override
        public void onDiscoveryJobCompleting(DiscoveryJob discoveryJob) {
            assertThat(discoveryJob).isNotNull();
            assertThat(discoveryJob.getStatus()).isSameAs(STARTED);
            calls.add(new Call(Call.Type.COMPLETING));
        }

        @Override
        public void onDiscoveryJobCompleted(DiscoveryJob discoveryJob) {
            assertThat(discoveryJob).isNotNull();
            assertThat(discoveryJob.getStatus()).isSameAs(COMPLETE);
            assertThat(discoveryJob.getLogMessage()).isNotNull();
            calls.add(new Call(Call.Type.COMPLETED));
        }

        @Override
        public void onDiscoveryJobModerating(DiscoveryJob discoveryJob) {
            assertThat(discoveryJob).isNotNull();
            assertThat(discoveryJob.getStatus()).isSameAs(STARTED);
            calls.add(new Call(Call.Type.MODERATING));
        }

        @Override
        public void onDiscoveryJobModerated(DiscoveryJob discoveryJob) {
            assertThat(discoveryJob).isNotNull();
            assertThat(discoveryJob.getStatus()).isSameAs(MODERATE);
            assertThat(discoveryJob.getLogMessage()).isNotNull();
            calls.add(new Call(Call.Type.MODERATED));
        }

        @Override
        public void onDiscoveryJobFailing(DiscoveryJob discoveryJob) {
            assertThat(discoveryJob).isNotNull();
            assertThat(discoveryJob.getStatus()).isSameAs(STARTED);
            calls.add(new Call(Call.Type.FAILING));
        }

        @Override
        public void onDiscoveryJobFailed(DiscoveryJob discoveryJob) {
            assertThat(discoveryJob).isNotNull();
            assertThat(discoveryJob.getStatus()).isSameAs(FAILED);
            assertThat(discoveryJob.getLogMessage()).isNotNull();
            calls.add(new Call(Call.Type.FAILED));
        }

        @Override
        public void onDiscoveryJobInterrupting(DiscoveryJob discoveryJob) {
            assertThat(discoveryJob).isNotNull();
            assertThat(discoveryJob.getStatus()).isSameAs(STARTED);
            calls.add(new Call(Call.Type.INTERRUPTING));
        }

        @Override
        public void onDiscoveryJobInterrupted(DiscoveryJob discoveryJob) {
            assertThat(discoveryJob).isNotNull();
            assertThat(discoveryJob.getStatus()).isSameAs(INTERRUPTED);
            calls.add(new Call(Call.Type.INTERRUPTED));
        }
    }

    private static class ThrowingDiscoveryJobServiceObserver implements DiscoveryJobService.Observer {

        private int callCount = 0;

        public int getCallCount() {
            return callCount;
        }

        @Override
        public void onDiscoveryJobStarting(DiscoveryJob discoveryJob) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onDiscoveryJobStarted(DiscoveryJob discoveryJob) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onDiscoveryJobProgress(DiscoveryJobProgress discoveryJobProgress) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onDiscoveryJobCompleting(DiscoveryJob discoveryJob) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onDiscoveryJobCompleted(DiscoveryJob discoveryJob) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onDiscoveryJobModerating(DiscoveryJob discoveryJob) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onDiscoveryJobModerated(DiscoveryJob discoveryJob) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onDiscoveryJobFailing(DiscoveryJob discoveryJob) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onDiscoveryJobFailed(DiscoveryJob discoveryJob) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onDiscoveryJobInterrupting(DiscoveryJob discoveryJob) {
            callCount++;
            throw new RuntimeException();
        }

        @Override
        public void onDiscoveryJobInterrupted(DiscoveryJob discoveryJob) {
            callCount++;
            throw new RuntimeException();
        }
    }
}
