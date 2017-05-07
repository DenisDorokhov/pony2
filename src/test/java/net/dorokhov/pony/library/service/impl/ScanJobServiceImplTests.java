package net.dorokhov.pony.library.service.impl;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.common.TransactionalTaskExecutor;
import net.dorokhov.pony.config.service.ConfigService;
import net.dorokhov.pony.fixture.ScanJobFixtures;
import net.dorokhov.pony.fixture.ScanResultFixtures;
import net.dorokhov.pony.fixture.TransactionTemplateFixtures;
import net.dorokhov.pony.fixture.TransactionalTaskExecutorFixtures;
import net.dorokhov.pony.library.domain.ScanJob;
import net.dorokhov.pony.library.domain.ScanResult;
import net.dorokhov.pony.library.domain.ScanStatus;
import net.dorokhov.pony.library.domain.ScanType;
import net.dorokhov.pony.library.repository.ScanJobRepository;
import net.dorokhov.pony.library.service.command.EditCommand;
import net.dorokhov.pony.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony.library.service.exception.LibraryNotDefinedException;
import net.dorokhov.pony.library.service.exception.NoScanEditCommandException;
import net.dorokhov.pony.library.service.exception.SongNotFoundException;
import net.dorokhov.pony.library.service.impl.audio.domain.WritableAudioData;
import net.dorokhov.pony.library.service.impl.scan.Scanner;
import net.dorokhov.pony.log.domain.LogMessage;
import net.dorokhov.pony.log.service.LogService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ScanJobServiceImplTests {
    
    @InjectMocks
    private ScanJobServiceImpl scanJobService;

    @Mock
    private ScanJobRepository scanJobRepository;
    @Mock
    private ConfigService configService;
    @Mock
    private Scanner scanner;
    @Mock
    private LogService logService;
    
    @Spy
    @SuppressWarnings("unused")
    private final TransactionalTaskExecutor transactionalTaskExecutor = TransactionalTaskExecutorFixtures.get();
    @Spy
    @SuppressWarnings("unused")
    private final TransactionTemplate transactionTemplate = TransactionTemplateFixtures.get();

    @Before
    public void setUp() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
    }

    @After
    public void tearDown() throws Exception {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    public void markCurrentJobsAsInterrupted() throws Exception {

        Pageable firstPageable = new PageRequest(0, 2);
        
        List<Page<ScanJob>> pages = new ArrayList<>();
        pages.add(new PageImpl<>(ImmutableList.of(ScanJobFixtures.full(), ScanJobFixtures.full()), firstPageable, 3));
        pages.add(new PageImpl<>(ImmutableList.of(ScanJobFixtures.full()), new PageRequest(1, 2), 3));
        pages.add(new PageImpl<>(ImmutableList.of()));
        
        given(scanJobRepository.findByStatusIn(any(), any())).willAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(1);
            return pages.get(pageable.getPageNumber());
        });
        
        scanJobService.markCurrentJobsAsInterrupted();

        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());
        
        savedScanJob.getAllValues().forEach(scanJob -> assertThat(scanJob.getStatus()).isEqualTo(ScanJob.Status.INTERRUPTED));
    }

    @Test
    public void failMarkingCurrentHobsAsInterruptedWhenScanIsRunning() throws Exception {
        given(scanner.getStatus()).willReturn(buildScanStatus());
        assertThatThrownBy(() -> scanJobService.markCurrentJobsAsInterrupted()).isInstanceOf(ConcurrentScanException.class);
    }

    @Test
    public void markCurrentJobsAsInterruptedOnStartup() throws Exception {
        given(scanJobRepository.findByStatusIn(any(), any())).willReturn(new PageImpl<>(ImmutableList.of()));
        ScanJobServiceImpl spy = Mockito.spy(scanJobService);
        spy.run(new DefaultApplicationArguments(new String[0]));
        verify(spy).markCurrentJobsAsInterrupted();
    }

    @Test
    public void getAll() throws Exception {
        Page<ScanJob> page = new PageImpl<>(ImmutableList.of());
        given(scanJobRepository.findAll((Pageable) any())).willReturn(page);
        assertThat(scanJobService.getAll(new PageRequest(0, 10))).isSameAs(page);
    }

    @Test
    public void getById() throws Exception {
        ScanJob scanJob = ScanJobFixtures.full();
        given(scanJobRepository.findOne(any())).willReturn(scanJob);
        assertThat(scanJobService.getById(1L)).isSameAs(scanJob);
    }

    @Test
    public void getScanStatus() throws Exception {
        ScanStatus scanStatus = buildScanStatus();
        given(scanner.getStatus()).willReturn(scanStatus);
        assertThat(scanJobService.getScanStatus()).isSameAs(scanStatus);
    }

    @Test
    public void executeScanJob() throws Exception {
        
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of(new File("someFolder")));
        given(logService.info(any(), any(), any())).willReturn(buildLogMessage());
        given(scanJobRepository.save((ScanJob) any())).willAnswer(invocation -> invocation.getArgument(0));
        ScanResult scanResult = ScanResultFixtures.get(ScanType.FULL);
        given(scanner.scan(any())).willReturn(scanResult);
        
        ScanJob scanJobStarting = scanJobService.startScanJob();
        assertThat(scanJobStarting.getScanType()).isEqualTo(ScanType.FULL);
        assertThat(scanJobStarting.getStatus()).isEqualTo(ScanJob.Status.STARTING);
        assertThat(scanJobStarting.getLogMessage()).isNotNull();
        assertThat(scanJobStarting.getScanResult()).isNull();
        verify(logService).info(any(), any(), any());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        
        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());
        verify(logService, times(3)).info(any(), any(), any());

        ScanJob scanJobStarted = savedScanJob.getAllValues().get(1);
        ScanJob scanJobComplete = savedScanJob.getAllValues().get(2);
        
        assertThat(scanJobStarted.getScanType()).isEqualTo(ScanType.FULL);
        assertThat(scanJobStarted.getStatus()).isEqualTo(ScanJob.Status.STARTED);
        assertThat(scanJobStarted.getLogMessage()).isNotNull();
        assertThat(scanJobStarted.getScanResult()).isNull();
        
        assertThat(scanJobComplete.getScanType()).isEqualTo(ScanType.FULL);
        assertThat(scanJobComplete.getStatus()).isEqualTo(ScanJob.Status.COMPLETE);
        assertThat(scanJobComplete.getLogMessage()).isNotNull();
        assertThat(scanJobComplete.getScanResult()).isSameAs(scanResult);
    }

    @Test
    public void executeEditJob() throws Exception {

        given(logService.info(any(), any(), any())).willReturn(buildLogMessage());
        given(scanJobRepository.save((ScanJob) any())).willAnswer(invocation -> invocation.getArgument(0));
        ScanResult scanResult = ScanResultFixtures.get(ScanType.EDIT);
        given(scanner.edit(any())).willReturn(scanResult);

        ScanJob scanJobStarting = scanJobService.startEditJob(ImmutableList.of(new EditCommand(1L, buildWritableAudioData())));
        assertThat(scanJobStarting.getScanType()).isEqualTo(ScanType.EDIT);
        assertThat(scanJobStarting.getStatus()).isEqualTo(ScanJob.Status.STARTING);
        assertThat(scanJobStarting.getLogMessage()).isNotNull();
        assertThat(scanJobStarting.getScanResult()).isNull();
        verify(logService).info(any(), any(), any());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());
        verify(logService, times(3)).info(any(), any(), any());

        ScanJob scanJobStarted = savedScanJob.getAllValues().get(1);
        ScanJob scanJobComplete = savedScanJob.getAllValues().get(2);

        assertThat(scanJobStarted.getScanType()).isEqualTo(ScanType.EDIT);
        assertThat(scanJobStarted.getStatus()).isEqualTo(ScanJob.Status.STARTED);
        assertThat(scanJobStarted.getLogMessage()).isNotNull();
        assertThat(scanJobStarted.getScanResult()).isNull();

        assertThat(scanJobComplete.getScanType()).isEqualTo(ScanType.EDIT);
        assertThat(scanJobComplete.getStatus()).isEqualTo(ScanJob.Status.COMPLETE);
        assertThat(scanJobComplete.getLogMessage()).isNotNull();
        assertThat(scanJobComplete.getScanResult()).isSameAs(scanResult);
    }

    @Test
    public void startAutoScanJobByInterval() throws Exception {
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of(new File("someFolder")));
        given(configService.getAutoScanInterval()).willReturn(24 * 60 * 60);
        given(scanJobRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of(ScanJobFixtures.builder(ScanType.FULL)
                .creationDate(LocalDateTime.now().minusDays(2))
                .updateDate(null)
                .build())));
        assertThat(scanJobService.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobRepository).save((ScanJob) any());
    }

    @Test
    public void startAutoScanJobIfRunningFirstTime() throws Exception {
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of(new File("someFolder")));
        given(configService.getAutoScanInterval()).willReturn(24 * 60 * 60);
        given(scanJobRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of()));
        assertThat(scanJobService.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobRepository).save((ScanJob) any());
    }

    @Test
    public void skipAutoScanJobIfAutoScanIsOff() throws Exception {
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of(new File("someFolder")));
        given(configService.getAutoScanInterval()).willReturn(null);
        assertThat(scanJobService.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobRepository, never()).save((ScanJob) any());
    }

    @Test
    public void skipAutoScanJobIfLibraryIsAlreadyBeingScanned() throws Exception {
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of(new File("someFolder")));
        given(scanner.getStatus()).willReturn(buildScanStatus());
        assertThat(scanJobService.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobRepository, never()).save((ScanJob) any());
    }

    @Test
    public void skipAutoScanJobIfNoLibraryFoldersDefined() throws Exception {
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of());
        assertThat(scanJobService.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobRepository, never()).save((ScanJob) any());
    }

    @Test
    public void skipAutoScanJobByInterval() throws Exception {
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of(new File("someFolder")));
        given(configService.getAutoScanInterval()).willReturn(24 * 60 * 60);
        given(scanJobRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of(ScanJobFixtures.full())));
        assertThat(scanJobService.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobRepository, never()).save((ScanJob) any());
    }

    @Test
    public void failScanJobIfLibraryNotDefined() throws Exception {
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of());
        assertThatThrownBy(() -> scanJobService.startScanJob()).isInstanceOf(LibraryNotDefinedException.class);
    }

    @Test
    public void failScanJobOnIOException() throws Exception {
        doTestFailScanJobOnExceptionDuringScan(new IOException());
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void failScanJobOnConcurrentScanException() throws Exception {
        doTestFailScanJobOnExceptionDuringScan(new ConcurrentScanException());
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void failScanJobOnUnexpectedException() throws Exception {
        doTestFailScanJobOnExceptionDuringScan(new RuntimeException());
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void failEditJobIfNoScanEditCommand() throws Exception {
        assertThatThrownBy(() -> scanJobService.startEditJob(ImmutableList.of())).isInstanceOf(NoScanEditCommandException.class);
    }

    @Test
    public void failEditJobOnSongNotFoundException() throws Exception {
        doTestFailEditJobOnExceptionDuringScan(new SongNotFoundException(1L));
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void failEditJobOnIOException() throws Exception {
        doTestFailEditJobOnExceptionDuringScan(new IOException());
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void failEditJobOnConcurrentScanException() throws Exception {
        doTestFailEditJobOnExceptionDuringScan(new ConcurrentScanException());
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void failEditJobOnUnexpectedException() throws Exception {
        doTestFailEditJobOnExceptionDuringScan(new RuntimeException());
        verify(logService).error(any(), any(), any());
    }
    
    private void doTestFailScanJobOnExceptionDuringScan(Exception e) throws Exception {
        
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of(new File("someFolder")));
        given(logService.error(any(), any(), any())).willReturn(buildLogMessage());
        given(scanJobRepository.save((ScanJob) any())).willAnswer(invocation -> invocation.getArgument(0));
        given(scanJobRepository.findOne(any())).willReturn(ScanJobFixtures.full());
        given(scanner.scan(any())).willThrow(e);

        scanJobService.startScanJob();
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());
        verify(logService, times(2)).info(any(), any(), any());

        ScanJob scanJobFailed = savedScanJob.getValue();
        assertThat(scanJobFailed.getScanType()).isEqualTo(ScanType.FULL);
        assertThat(scanJobFailed.getStatus()).isEqualTo(ScanJob.Status.FAILED);
        assertThat(scanJobFailed.getLogMessage()).isNotNull();
        assertThat(scanJobFailed.getScanResult()).isNull();
    }
    
    private void doTestFailEditJobOnExceptionDuringScan(Exception e) throws Exception {
        
        given(logService.error(any(), any(), any())).willReturn(buildLogMessage());
        given(scanJobRepository.save((ScanJob) any())).willAnswer(invocation -> invocation.getArgument(0));
        given(scanJobRepository.findOne(any())).willReturn(ScanJobFixtures.edit());
        given(scanner.edit(any())).willThrow(e);

        scanJobService.startEditJob(ImmutableList.of(new EditCommand(1L, buildWritableAudioData())));
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<ScanJob> savedScanJob = ArgumentCaptor.forClass(ScanJob.class);
        verify(scanJobRepository, times(3)).save(savedScanJob.capture());
        verify(logService, times(2)).info(any(), any(), any());

        ScanJob scanJobFailed = savedScanJob.getValue();
        assertThat(scanJobFailed.getScanType()).isEqualTo(ScanType.EDIT);
        assertThat(scanJobFailed.getStatus()).isEqualTo(ScanJob.Status.FAILED);
        assertThat(scanJobFailed.getLogMessage()).isNotNull();
        assertThat(scanJobFailed.getScanResult()).isNull();
    }
    
    private ScanStatus buildScanStatus() {
        return new ScanStatus(ScanStatus.Step.FULL_PREPARING, ImmutableList.of(), 0.0);
    }
    
    private LogMessage buildLogMessage() {
        return LogMessage.builder()
                .type(LogMessage.Level.DEBUG)
                .pattern("someCode")
                .text("someText")
                .build();
    }
    
    private WritableAudioData buildWritableAudioData() {
        return WritableAudioData.builder().build();
    }
}
