package net.dorokhov.pony.library.service.impl;

import net.dorokhov.pony.common.TransactionalTaskExecutor;
import net.dorokhov.pony.config.service.ConfigService;
import net.dorokhov.pony.library.domain.ScanJob;
import net.dorokhov.pony.library.domain.ScanJob.Status;
import net.dorokhov.pony.library.domain.ScanResult;
import net.dorokhov.pony.library.domain.ScanStatus;
import net.dorokhov.pony.library.domain.ScanType;
import net.dorokhov.pony.library.repository.ScanJobRepository;
import net.dorokhov.pony.library.service.ScanJobService;
import net.dorokhov.pony.library.service.command.EditCommand;
import net.dorokhov.pony.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony.library.service.exception.LibraryNotDefinedException;
import net.dorokhov.pony.library.service.exception.NoScanEditCommandException;
import net.dorokhov.pony.library.service.exception.SongNotFoundException;
import net.dorokhov.pony.library.service.impl.scan.Scanner;
import net.dorokhov.pony.log.domain.LogMessage;
import net.dorokhov.pony.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Service
public class ScanJobServiceImpl implements ScanJobService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ScanJobRepository scanJobRepository;
    private final ConfigService configService;
    private final Scanner scanner;
    private final LogService logService;
    private final TransactionalTaskExecutor transactionalTaskExecutor;
    private final TransactionTemplate transactionTemplate;

    public ScanJobServiceImpl(ScanJobRepository scanJobRepository,
                              ConfigService configService,
                              Scanner scanner,
                              LogService logService,
                              TransactionalTaskExecutor transactionalTaskExecutor,
                              TransactionTemplate transactionTemplate) {
        this.scanJobRepository = scanJobRepository;
        this.configService = configService;
        this.scanner = scanner;
        this.logService = logService;
        this.transactionalTaskExecutor = transactionalTaskExecutor;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScanJob> getAll(Pageable pageable) {
        return scanJobRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ScanJob getById(Long id) {
        return scanJobRepository.findOne(id);
    }

    @Override
    public ScanStatus getScanStatus() {
        return scanner.getStatus();
    }

    @Override
    @Transactional
    public ScanJob startScanJob() throws LibraryNotDefinedException {
        return doStartScanJob(configService.getLibraryFolders());
    }

    @Override
    @Transactional
    public ScanJob startEditJob(List<EditCommand> commands) throws NoScanEditCommandException {

        if (commands.size() == 0) {
            throw new NoScanEditCommandException();
        }

        LogMessage logStarting = logService.info(logger, "Starting edit job for {} songs...", commands.size());
        ScanJob startingJob = scanJobRepository.save(ScanJob.builder()
                .scanType(ScanType.EDIT)
                .status(Status.STARTING)
                .logMessage(logStarting)
                .build());

        registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                transactionalTaskExecutor.execute(() -> {
                    try {
                        doEditJob(startingJob, commands);
                    } catch (Exception e) {
                        // Ensure transaction is not rolled back.
                        transactionTemplate.execute(status -> {
                            LogMessage logFailed = logService.error(logger, "Unexpected error occurred when performing edit job.", e);
                            ScanJob failedJob = scanJobRepository.findOne(startingJob.getId());
                            scanJobRepository.save(ScanJob.builder(failedJob)
                                    .status(Status.FAILED)
                                    .logMessage(logFailed)
                                    .build());
                            return null;
                        });
                    }
                });
            }
        });

        return startingJob;
    }

    @Override
    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    @Transactional
    @Nullable
    public ScanJob startAutoScanJobIfNeeded() {

        logger.debug("Checking if automatic scan needed...");
        boolean shouldScan = false;

        List<File> libraryFolders = configService.getLibraryFolders();
        if (libraryFolders.size() > 0) {
            if (scanner.getStatus() == null) {
                Integer autoScanInterval = configService.getAutoScanInterval();
                if (autoScanInterval != null) {
                    shouldScan = shouldAutoScanByInterval(autoScanInterval);
                } else {
                    logger.debug("Automatic scan is off.");
                }
            } else {
                logger.debug("Library is already being scanned.");
            }
        } else {
            logger.debug("No library folders defined.");
        }

        if (shouldScan) {
            logger.info("Starting automatic scan...");
            try {
                return doStartScanJob(libraryFolders);
            } catch (LibraryNotDefinedException e) {
                throw new IllegalStateException("Library is not defined. Race condition?", e);
            }
        }
        return null;
    }

    @Nullable
    private ScanJob doStartScanJob(List<File> targetFolders) throws LibraryNotDefinedException {

        if (targetFolders.size() == 0) {
            throw new LibraryNotDefinedException();
        }

        List<String> targetPaths = targetFolders.stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
        LogMessage logStarting = logService.info(logger, "Starting scan job for '{}'...", targetPaths);
        ScanJob startingJob = scanJobRepository.save(ScanJob.builder()
                .scanType(ScanType.FULL)
                .status(Status.STARTING)
                .logMessage(logStarting)
                .build());

        registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                transactionalTaskExecutor.execute(() -> {
                    try {
                        doScanJob(startingJob, targetFolders);
                    } catch (Exception e) {
                        // Ensure transaction is not rolled back.
                        transactionTemplate.execute(status -> {
                            LogMessage logFailed = logService.error(logger, "Unexpected error occurred when performing scan job.", e);
                            ScanJob failedJob = scanJobRepository.findOne(startingJob.getId());
                            scanJobRepository.save(ScanJob.builder(failedJob)
                                    .status(Status.FAILED)
                                    .logMessage(logFailed)
                                    .build());
                            return null;
                        });
                    }
                });
            }
        });

        return startingJob;
    }

    private void doScanJob(ScanJob scanJob, List<File> targetFolders) {

        List<String> targetPaths = targetFolders.stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());

        // Ensure transaction is not rolled back.
        ScanJob startingScanJob = scanJob;
        scanJob = transactionTemplate.execute(status -> {
            LogMessage logStarted = logService.info(logger, "Started scan job for '{}'.", targetPaths);
            return scanJobRepository.save(ScanJob.builder(startingScanJob)
                    .status(Status.STARTED)
                    .logMessage(logStarted)
                    .build());
        });

        ScanResult result = null;
        LogMessage logMessage;
        try {
            result = scanner.scan(targetFolders);
            logMessage = logService.info(logger, "Scan job complete for '{}'.", targetPaths);
        } catch (IOException e) {
            logMessage = logService.error(logger, "Scan job failed due to I/O error.", e);
        } catch (ConcurrentScanException e) {
            logMessage = logService.error(logger, "Library is already being scanned.");
        }

        scanJobRepository.save(ScanJob.builder(scanJob)
                .status(result != null ? Status.COMPLETE : Status.FAILED)
                .scanResult(result)
                .logMessage(logMessage)
                .build());
    }

    private void doEditJob(ScanJob scanJob, List<EditCommand> commands) {

        // Ensure transaction is not rolled back.
        ScanJob startingScanJob = scanJob;
        scanJob = transactionTemplate.execute(status -> {
            LogMessage logStarted = logService.info(logger, "Started edit job for {} songs...", commands.size());
            return scanJobRepository.save(ScanJob.builder(startingScanJob)
                    .status(Status.STARTED)
                    .logMessage(logStarted)
                    .build());
        });

        ScanResult result = null;
        LogMessage logMessage;
        try {
            result = scanner.edit(commands);
            logMessage = logService.info(logger, "Edit job complete for {} songs.", commands.size());
        } catch (SongNotFoundException e) {
            logMessage = logService.error(logger, "Song '{}' not found.", e.getId());
        } catch (IOException e) {
            logMessage = logService.error(logger, "Edit job failed due to I/O error.", e);
        } catch (ConcurrentScanException e) {
            logMessage = logService.error(logger, "Library is already scanning.");
        }

        scanJobRepository.save(ScanJob.builder(scanJob)
                .status(result != null ? Status.COMPLETE : Status.FAILED)
                .scanResult(result)
                .logMessage(logMessage)
                .build());
    }

    private boolean shouldAutoScanByInterval(int autoScanInterval) {
        Page<ScanJob> page = getAll(new PageRequest(0, 1, Sort.Direction.DESC, "creationDate", "updateDate"));
        ScanJob lastJob = page.getTotalElements() > 0 ? page.getContent().get(0) : null;
        if (lastJob != null) {
            LocalDateTime jobDate = lastJob.getUpdateDate();
            if (jobDate == null) {
                jobDate = lastJob.getCreationDate();
            }
            long secondsSinceLastScan = jobDate.until(LocalDateTime.now(), ChronoUnit.SECONDS);
            if (secondsSinceLastScan >= autoScanInterval) {
                return true;
            } else {
                logger.trace("Too early for automatic scan.");
            }
        } else {
            logger.trace("Library was never scanned before.");
            return true;
        }
        return false;
    }
}
