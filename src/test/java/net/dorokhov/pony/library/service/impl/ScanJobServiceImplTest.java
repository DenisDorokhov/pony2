package net.dorokhov.pony.library.service.impl;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.common.TransactionalTaskExecutor;
import net.dorokhov.pony.config.service.ConfigService;
import net.dorokhov.pony.fixture.TransactionTemplateFixtures;
import net.dorokhov.pony.fixture.TransactionalTaskExecutorFixtures;
import net.dorokhov.pony.library.domain.ScanJob;
import net.dorokhov.pony.library.domain.ScanResult;
import net.dorokhov.pony.library.domain.ScanStatus;
import net.dorokhov.pony.library.domain.ScanStatus.Progress.Step;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
        initSynchronization();
    }

    @After
    public void tearDown() throws Exception {
        clearSynchronization();
    }

    @Test
    public void shouldGetAll() throws Exception {
        Page<ScanJob> page = new PageImpl<>(ImmutableList.of());
        given(scanJobRepository.findAll((Pageable) any())).willReturn(page);
        assertThat(scanJobService.getAll(new PageRequest(0, 10))).isSameAs(page);
    }

    @Test
    public void shouldGetById() throws Exception {
        ScanJob scanJob = scanJobFull();
        given(scanJobRepository.findOne(any())).willReturn(scanJob);
        assertThat(scanJobService.getById(1L)).isSameAs(scanJob);
    }

    @Test
    public void shouldGetScanStatus() throws Exception {
        ScanStatus scanStatus = scanStatus();
        given(scanner.getStatus()).willReturn(scanStatus);
        assertThat(scanJobService.getScanStatus()).isSameAs(scanStatus);
    }

    @Test
    public void shouldExecuteScanJob() throws Exception {
        
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of(new File("someFolder")));
        given(logService.info(any(), any(), any())).willReturn(logMessage());
        given(scanJobRepository.save((ScanJob) any())).willAnswer(returnsFirstArg());
        ScanResult scanResult = scanResult(ScanType.FULL);
        given(scanner.scan(any())).willReturn(scanResult);
        
        ScanJob scanJobStarting = scanJobService.startScanJob();
        assertThat(scanJobStarting.getScanType()).isEqualTo(ScanType.FULL);
        assertThat(scanJobStarting.getStatus()).isEqualTo(ScanJob.Status.STARTING);
        assertThat(scanJobStarting.getLogMessage()).isNotNull();
        assertThat(scanJobStarting.getScanResult()).isNull();
        verify(logService).info(any(), any(), any());

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        
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
    public void shouldExecuteEditJob() throws Exception {

        given(logService.info(any(), any(), any())).willReturn(logMessage());
        given(scanJobRepository.save((ScanJob) any())).willAnswer(returnsFirstArg());
        ScanResult scanResult = scanResult(ScanType.EDIT);
        given(scanner.edit(any())).willReturn(scanResult);

        ScanJob scanJobStarting = scanJobService.startEditJob(ImmutableList.of(new EditCommand(1L, writableAudioData())));
        assertThat(scanJobStarting.getScanType()).isEqualTo(ScanType.EDIT);
        assertThat(scanJobStarting.getStatus()).isEqualTo(ScanJob.Status.STARTING);
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
        assertThat(scanJobStarted.getStatus()).isEqualTo(ScanJob.Status.STARTED);
        assertThat(scanJobStarted.getLogMessage()).isNotNull();
        assertThat(scanJobStarted.getScanResult()).isNull();

        assertThat(scanJobComplete.getScanType()).isEqualTo(ScanType.EDIT);
        assertThat(scanJobComplete.getStatus()).isEqualTo(ScanJob.Status.COMPLETE);
        assertThat(scanJobComplete.getLogMessage()).isNotNull();
        assertThat(scanJobComplete.getScanResult()).isSameAs(scanResult);
    }

    @Test
    public void shouldStartAutoScanJobByInterval() throws Exception {
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of(new File("someFolder")));
        given(configService.getAutoScanInterval()).willReturn(24 * 60 * 60);
        given(scanJobRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of(scanJobBuilder(ScanType.FULL)
                .creationDate(LocalDateTime.now().minusDays(2))
                .updateDate(null)
                .build())));
        assertThat(scanJobService.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobRepository).save((ScanJob) any());
    }

    @Test
    public void shouldStartAutoScanJobIfRunningFirstTime() throws Exception {
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of(new File("someFolder")));
        given(configService.getAutoScanInterval()).willReturn(24 * 60 * 60);
        given(scanJobRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of()));
        assertThat(scanJobService.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobRepository).save((ScanJob) any());
    }

    @Test
    public void shouldSkipAutoScanJobIfAutoScanIsOff() throws Exception {
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of(new File("someFolder")));
        given(configService.getAutoScanInterval()).willReturn(null);
        assertThat(scanJobService.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobRepository, never()).save((ScanJob) any());
    }

    @Test
    public void shouldSkipAutoScanJobIfLibraryIsAlreadyBeingScanned() throws Exception {
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of(new File("someFolder")));
        given(scanner.getStatus()).willReturn(scanStatus());
        assertThat(scanJobService.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobRepository, never()).save((ScanJob) any());
    }

    @Test
    public void shouldSkipAutoScanJobIfNoLibraryFoldersDefined() throws Exception {
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of());
        assertThat(scanJobService.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobRepository, never()).save((ScanJob) any());
    }

    @Test
    public void shouldSkipAutoScanJobByInterval() throws Exception {
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of(new File("someFolder")));
        given(configService.getAutoScanInterval()).willReturn(24 * 60 * 60);
        given(scanJobRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(ImmutableList.of(scanJobFull())));
        assertThat(scanJobService.startAutoScanJobIfNeeded()).isNull();
        verify(scanJobRepository, never()).save((ScanJob) any());
    }

    @Test
    public void shouldFailScanJobIfLibraryNotDefined() throws Exception {
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of());
        assertThatThrownBy(() -> scanJobService.startScanJob()).isInstanceOf(LibraryNotDefinedException.class);
    }

    @Test
    public void shouldFailScanJobOnIOException() throws Exception {
        doTestFailScanJobOnExceptionDuringScan(new IOException());
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldFailScanJobOnConcurrentScanException() throws Exception {
        doTestFailScanJobOnExceptionDuringScan(new ConcurrentScanException());
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldFailScanJobOnUnexpectedException() throws Exception {
        doTestFailScanJobOnExceptionDuringScan(new RuntimeException());
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldFailEditJobIfNoScanEditCommand() throws Exception {
        assertThatThrownBy(() -> scanJobService.startEditJob(ImmutableList.of())).isInstanceOf(NoScanEditCommandException.class);
    }

    @Test
    public void shouldFailEditJobOnSongNotFoundException() throws Exception {
        doTestFailEditJobOnExceptionDuringScan(new SongNotFoundException(1L));
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldFailEditJobOnIOException() throws Exception {
        doTestFailEditJobOnExceptionDuringScan(new IOException());
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldFailEditJobOnConcurrentScanException() throws Exception {
        doTestFailEditJobOnExceptionDuringScan(new ConcurrentScanException());
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldFailEditJobOnUnexpectedException() throws Exception {
        doTestFailEditJobOnExceptionDuringScan(new RuntimeException());
        verify(logService).error(any(), any(), any());
    }
    
    private void doTestFailScanJobOnExceptionDuringScan(Exception e) throws Exception {
        
        given(configService.getLibraryFolders()).willReturn(ImmutableList.of(new File("someFolder")));
        given(logService.error(any(), any(), any())).willReturn(logMessage());
        given(scanJobRepository.save((ScanJob) any())).willAnswer(returnsFirstArg());
        given(scanJobRepository.findOne(any())).willReturn(scanJobFull());
        given(scanner.scan(any())).willThrow(e);

        scanJobService.startScanJob();
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

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
        
        given(logService.error(any(), any(), any())).willReturn(logMessage());
        given(scanJobRepository.save((ScanJob) any())).willAnswer(returnsFirstArg());
        given(scanJobRepository.findOne(any())).willReturn(scanJobEdit());
        given(scanner.edit(any())).willThrow(e);

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
    }
    
    private ScanStatus scanStatus() {
        return new ScanStatus(true, new ScanStatus.Progress(Step.FULL_PREPARING, ImmutableList.of(), 0.0));
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

    private ScanJob scanJobFull() {
        return scanJob(ScanType.FULL);
    }

    private ScanJob scanJobEdit() {
        return scanJob(ScanType.EDIT);
    }

    private ScanJob scanJob(ScanType scanType) {
        return scanJobBuilder(scanType).build();
    }

    private ScanJob.Builder scanJobBuilder(ScanType scanType) {
        return ScanJob.builder()
                .id(1L)
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .status(ScanJob.Status.STARTING)
                .scanType(scanType);
    }
    
    private ScanResult scanResult(ScanType scanType) {
        return ScanResult.builder()
                .scanType(scanType)
                .targetPaths(ImmutableList.of())
                .failedPaths(ImmutableList.of())
                .duration(10L)
                .songSize(1000L)
                .artworkSize(5L)
                .genreCount(1L)
                .artistCount(2L)
                .albumCount(3L)
                .songCount(4L)
                .artworkCount(5L)
                .audioFileCount(32L)
                .imageFileCount(64L)
                .createdArtistCount(7L)
                .updatedArtistCount(8L)
                .deletedArtistCount(9L)
                .createdAlbumCount(10L)
                .updatedAlbumCount(11L)
                .deletedAlbumCount(12L)
                .createdGenreCount(13L)
                .updatedGenreCount(14L)
                .deletedGenreCount(15L)
                .createdSongCount(16L)
                .updatedSongCount(17L)
                .deletedSongCount(18L)
                .createdArtworkCount(19L)
                .deletedArtworkCount(20L)
                .build();
    }
}
