package net.dorokhov.pony2.core.library.service;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.library.domain.ScanJob;
import net.dorokhov.pony2.api.library.domain.ScanJob.Status;
import net.dorokhov.pony2.api.library.domain.ScanJobProgress;
import net.dorokhov.pony2.api.library.domain.ScanResult;
import net.dorokhov.pony2.api.library.domain.ScanType;
import net.dorokhov.pony2.api.library.service.ScanJobService;
import net.dorokhov.pony2.api.library.service.command.EditCommand;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.repository.ScanJobRepository;
import net.dorokhov.pony2.core.library.service.scan.LibraryScanner;
import net.dorokhov.pony2.core.library.service.scan.exception.SongNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.synchronizedSet;
import static net.dorokhov.pony2.core.library.LibraryConfig.SCAN_JOB_EXECUTOR;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Service
public class ScanJobServiceImpl implements ScanJobService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ScanJobRepository scanJobRepository;
    private final ConfigService configService;
    private final LibraryScanner libraryScanner;
    private final LogService logService;
    private final Executor scanJobExecutor;

    private final TransactionTemplate transactionTemplate;

    private final Set<Observer> observers = synchronizedSet(new LinkedHashSet<>());

    private final AtomicReference<ScanJobProgress> scanJobProgressReference = new AtomicReference<>();

    private final Semaphore scanJobSemaphore = new Semaphore(1);

    public ScanJobServiceImpl(
            ScanJobRepository scanJobRepository,
            ConfigService configService,
            LibraryScanner libraryScanner,
            LogService logService,
            @Qualifier(SCAN_JOB_EXECUTOR) Executor scanJobExecutor,
            PlatformTransactionManager transactionManager
    ) {

        this.scanJobRepository = scanJobRepository;
        this.configService = configService;
        this.libraryScanner = libraryScanner;
        this.logService = logService;
        this.scanJobExecutor = scanJobExecutor;

        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    @Override
    public void addObserver(Observer observer) {
        observers.add(checkNotNull(observer));
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(checkNotNull(observer));
    }

    @Override
    @Nullable
    public Optional<ScanJobProgress> getCurrentScanJobProgress() {
        return Optional.ofNullable(scanJobProgressReference.get());
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public Optional<ScanJobProgress> getScanJobProgress(String id) {
        ScanJobProgress scanJobProgress = scanJobProgressReference.get();
        if (scanJobProgress != null && id.equals(scanJobProgress.getScanJob().getId())) {
            return Optional.of(scanJobProgress);
        } else {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScanJob> getAll(Pageable pageable) {
        return scanJobRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ScanJob> getById(String id) {
        return scanJobRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public Optional<ScanJob> getLastSuccessfulJob() {
        return scanJobRepository.findFirstByStatusOrderByUpdateDateDesc(Status.COMPLETE);
    }

    @Override
    @Transactional
    public ScanJob startScanJob() throws ConcurrentScanException {
        return doStartScanJob(configService.getLibraryFolders());
    }

    @Override
    @Transactional
    public ScanJob startEditJob(List<EditCommand> commands) throws ConcurrentScanException {

        if (!scanJobSemaphore.tryAcquire()) {
            throw new ConcurrentScanException();
        }

        List<String> targetPaths = commands.stream()
                .map(EditCommand::getSongFilePath)
                .toList();
        LogMessage logStarting = logService.info(logger, "Starting edit job for {}...", targetPaths);
        ScanJob scanJob = scanJobRepository.save(new ScanJob()
                .setScanType(ScanType.EDIT)
                .setStatus(Status.STARTING)
                .setTargetPaths(targetPaths)
                .setLogMessage(logStarting));

        registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                onScanJobStatusChange(scanJob);
                scanJobExecutor.execute(() -> {
                    ScanJob currentScanJob = scanJob;
                    try {
                        currentScanJob = changeScanJobStatusInTransaction(() -> {
                            LogMessage logStarted = logService.info(logger, "Started edit job for {} songs...", commands.size());
                            return scanJobRepository.save(scanJob
                                    .setStatus(Status.STARTED)
                                    .setLogMessage(logStarted));
                        });
                        doEditJob(currentScanJob, commands);
                    } catch (Exception e) {
                        final ScanJob failedScanJob = currentScanJob;
                        notifyObservers(observer -> observer.onScanJobFailing(failedScanJob));
                        changeScanJobStatusInTransaction(() -> {
                            LogMessage logFailed = logService.error(logger, "Unexpected error occurred when performing edit job.", e);
                            return scanJobRepository.save(
                                    scanJobRepository.findById(scanJob.getId()).orElseThrow()
                                            .setStatus(Status.FAILED)
                                            .setLogMessage(logFailed));
                        });
                    } finally {
                        scanJobProgressReference.set(null);
                        scanJobSemaphore.release();
                    }
                });
            }
        });

        return scanJob;
    }

    private ScanJob doStartScanJob(List<File> targetFolders) throws ConcurrentScanException {

        if (!scanJobSemaphore.tryAcquire()) {
            throw new ConcurrentScanException();
        }

        List<String> targetPaths = fetchAbsolutePaths(targetFolders);
        LogMessage logStarting = logService.info(logger, "Starting scan job for {}...", targetPaths);
        ScanJob scanJob = scanJobRepository.save(new ScanJob()
                .setScanType(ScanType.FULL)
                .setStatus(Status.STARTING)
                .setTargetPaths(targetPaths)
                .setLogMessage(logStarting));

        registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                onScanJobStatusChange(scanJob);
                scanJobExecutor.execute(() -> {
                    ScanJob currentScanJob = scanJob;
                    try {
                        currentScanJob = changeScanJobStatusInTransaction(() -> {
                            LogMessage logStarted = logService.info(logger, "Started scan job for {}.", targetPaths);
                            return scanJobRepository.save(scanJob
                                    .setStatus(Status.STARTED)
                                    .setLogMessage(logStarted));
                        });
                        doScanJob(currentScanJob, targetFolders);
                    } catch (Exception e) {
                        final ScanJob failedScanJob = currentScanJob;
                        notifyObservers(observer -> observer.onScanJobFailing(failedScanJob));
                        changeScanJobStatusInTransaction(() -> {
                            LogMessage logFailed = logService.error(logger, "Unexpected error occurred when performing scan job.", e);
                            return scanJobRepository.save(
                                    scanJobRepository.findById(scanJob.getId()).orElseThrow()
                                            .setStatus(Status.FAILED)
                                            .setLogMessage(logFailed));
                        });
                    } finally {
                        scanJobProgressReference.set(null);
                        scanJobSemaphore.release();
                    }
                });
            }
        });

        return scanJob;
    }

    private void doScanJob(ScanJob scanJob, List<File> targetFolders) {

        ScanResult result = null;
        Supplier<LogMessage> logMessage;
        try {
            result = libraryScanner.scan(targetFolders, scanProgress ->
                    onScanJobProgress(new ScanJobProgress(scanJob, scanProgress)));
            logMessage = () -> logService.info(logger, "Scan job complete for {}.", fetchAbsolutePaths(targetFolders));
        } catch (IOException e) {
            logMessage = () -> logService.error(logger, "Scan job failed due to I/O error.", e);
        }

        if (result != null) {
            notifyObservers(observer -> observer.onScanJobCompleting(scanJob));
        } else {
            notifyObservers(observer -> observer.onScanJobFailing(scanJob));
        }

        final ScanResult finalResult = result;
        final Supplier<LogMessage> finalLogMessage = logMessage;
        changeScanJobStatusInTransaction(() -> scanJobRepository.save(scanJob
                .setStatus(finalResult != null ? Status.COMPLETE : Status.FAILED)
                .setScanResult(finalResult)
                .setLogMessage(finalLogMessage.get())));
    }

    private void doEditJob(ScanJob scanJob, List<EditCommand> commands) {

        ScanResult result = null;
        Supplier<LogMessage> logMessage;
        try {
            result = libraryScanner.edit(commands, configService.getLibraryFolders(), scanProgress ->
                    onScanJobProgress(new ScanJobProgress(scanJob, scanProgress)));
            logMessage = () -> logService.info(logger, "Edit job complete for {} songs.", commands.size());
        } catch (SongNotFoundException e) {
            logMessage = () -> logService.error(logger, "Song '{}' not found.", e.getSongId());
        } catch (IOException e) {
            logMessage = () -> logService.error(logger, "Edit job failed due to I/O error.", e);
        }

        if (result != null) {
            notifyObservers(observer -> observer.onScanJobCompleting(scanJob));
        } else {
            notifyObservers(observer -> observer.onScanJobFailing(scanJob));
        }

        final ScanResult finalResult = result;
        final Supplier<LogMessage> finalLogMessage = logMessage;
        changeScanJobStatusInTransaction(() -> scanJobRepository.save(scanJob
                .setStatus(finalResult != null ? Status.COMPLETE : Status.FAILED)
                .setScanResult(finalResult)
                .setLogMessage(finalLogMessage.get())));
    }

    private ScanJob onScanJobStatusChange(ScanJob scanJob) {
        scanJobProgressReference.set(new ScanJobProgress(scanJob, null));
        switch (scanJob.getStatus()) {
            case STARTING:
                notifyObservers(observer -> observer.onScanJobStarting(scanJob));
                break;
            case STARTED:
                notifyObservers(observer -> observer.onScanJobStarted(scanJob));
                break;
            case COMPLETE:
                notifyObservers(observer -> observer.onScanJobCompleted(scanJob));
                break;
            case FAILED:
                notifyObservers(observer -> observer.onScanJobFailed(scanJob));
                break;
            default:
                throw new IllegalStateException("Unexpected scan job status.");
        }
        return scanJob;
    }

    private void onScanJobProgress(ScanJobProgress scanJobProgress) {
        scanJobProgressReference.set(scanJobProgress);
        notifyObservers(observer -> observer.onScanJobProgress(scanJobProgress));
    }

    private void notifyObservers(Consumer<Observer> handler) {
        for (Observer observer : new ArrayList<>(observers)) {
            try {
                handler.accept(observer);
            } catch (Exception e) {
                logger.error("Could not call progress observer {}.", observer, e);
            }
        }
    }

    private ScanJob changeScanJobStatusInTransaction(Supplier<ScanJob> handler) {
        return onScanJobStatusChange(transactionTemplate.execute(transactionStatus -> handler.get()));
    }

    private List<String> fetchAbsolutePaths(List<File> files) {
        return files.stream()
                .map(File::getAbsolutePath)
                .toList();
    }
}
