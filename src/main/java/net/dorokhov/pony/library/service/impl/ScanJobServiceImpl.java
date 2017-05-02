package net.dorokhov.pony.library.service.impl;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.common.PageWalker;
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
import net.dorokhov.pony.log.domain.LogMessage;
import net.dorokhov.pony.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
public class ScanJobServiceImpl implements ScanJobService, ApplicationRunner {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ScanJobRepository scanJobRepository;
    private final ConfigService configService;
    private final Scanner scanner;
    private final LogService logService;
    private final TaskExecutor taskExecutor;
    
    public ScanJobServiceImpl(ScanJobRepository scanJobRepository,
                              ConfigService configService,
                              Scanner scanner,
                              LogService logService,
                              TaskExecutor taskExecutor) {
        this.scanJobRepository = scanJobRepository;
        this.configService = configService;
        this.scanner = scanner;
        this.logService = logService;
        this.taskExecutor = taskExecutor;
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void markCurrentJobsAsInterrupted() throws ConcurrentScanException {
        
        if (scanner.getStatus() != null) {
            throw new ConcurrentScanException();
        }

        AtomicInteger interruptedJobsCount = new AtomicInteger();
        PageWalker.walk(new PageRequest(0, 100), scanJob -> {
            scanJobRepository.save(ScanJob.builder(scanJob)
                    .status(Status.INTERRUPTED)
                    .build());
            interruptedJobsCount.incrementAndGet();
        }, pageable -> scanJobRepository.findByStatusIn(ImmutableList.of(Status.STARTING, Status.STARTED), pageable));

        if (interruptedJobsCount.get() > 0) {
            logService.warn(log, "scanJobService.scanJobInterrupting", ImmutableList.of(String.valueOf(interruptedJobsCount.get())),
                    String.format("Interrupted %d scan job(s).", interruptedJobsCount.get()));
        }
    }

    @Override
    @Transactional(propagation = REQUIRES_NEW)
    public void run(ApplicationArguments args) throws Exception {
        markCurrentJobsAsInterrupted();
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
    @Nullable
    public ScanStatus getScanStatus() {
        return scanner.getStatus();
    }

    @Override
    @Transactional(propagation = REQUIRES_NEW)
    public ScanJob startScanJob() throws LibraryNotDefinedException {
        return doStartScanJob(configService.getLibraryFolders());
    }

    @Override
    @Transactional(propagation = REQUIRES_NEW)
    public ScanJob startEditJob(List<EditCommand> commands) throws NoScanEditCommandException {
        
        if (commands.size() == 0) {
            throw new NoScanEditCommandException();
        }
        
        LogMessage logStarting = logService.info(log, "scanJobService.editJobStarting", ImmutableList.of(String.valueOf(commands.size())),
                String.format("Starting edit job for %d songs...", commands.size()));
        ScanJob startingJob = scanJobRepository.save(ScanJob.builder()
                .scanType(ScanType.EDIT)
                .status(Status.STARTING)
                .logMessage(logStarting)
                .build());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                taskExecutor.execute(() -> {
                    try {
                        doEditJob(startingJob, commands);
                    } catch (Exception e) {
                        LogMessage logFailed = logService.error(log, "scanJobService.editJobFailed",
                                "Unexpected error occurred when performing edit job.", e);
                        ScanJob failedJob = scanJobRepository.findOne(startingJob.getId());
                        scanJobRepository.save(ScanJob.builder(failedJob)
                                .status(Status.FAILED)
                                .logMessage(logFailed)
                                .build());
                    }
                });
            }
        });
        
        return startingJob;
    }

    @Override
    @Transactional(propagation = REQUIRES_NEW)
    @Nullable
    public ScanJob startAutoScanJobIfNeeded() {
        
        log.debug("Checking if automatic scan needed...");
        boolean shouldScan = false;

        List<File> libraryFolders = configService.getLibraryFolders();
        if (libraryFolders.size() > 0) {
            if (scanner.getStatus() == null) {
                Integer autoScanInterval = configService.getAutoScanInterval();
                if (autoScanInterval != null) {
                    shouldScan = shouldAutoScanByInterval(autoScanInterval);
                } else {
                    log.trace("Automatic scan is off.");
                }
            } else {
                log.debug("Library is already being scanned.");
            }
        } else {
            log.debug("No library folders defined.");
        }

        if (shouldScan) {
            log.info("Starting automatic scan...");
            return doStartScanJob(libraryFolders);
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
        LogMessage logStarting = logService.info(log, "scanJobService.scanJobStarting", ImmutableList.of(targetPaths.toString()), 
                String.format("Starting scan job for '%s'...", targetPaths));
        ScanJob startingJob = scanJobRepository.save(ScanJob.builder()
                .scanType(ScanType.FULL)
                .status(Status.STARTING)
                .logMessage(logStarting)
                .build());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                taskExecutor.execute(() -> {
                    try {
                        doScanJob(startingJob, targetFolders);
                    } catch (Exception e) {
                        LogMessage logFailed = logService.error(log, "scanJobService.scanJobFailed",
                                "Unexpected error occurred when performing scan job.", e);
                        ScanJob failedJob = scanJobRepository.findOne(startingJob.getId());
                        scanJobRepository.save(ScanJob.builder(failedJob)
                                .status(Status.FAILED)
                                .logMessage(logFailed)
                                .build());
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

        LogMessage logStarted = logService.info(log, "scanJobService.scanJobStarted", ImmutableList.of(targetPaths.toString()), 
                String.format("Started scan job for '%s'.", targetPaths));
        scanJob = scanJobRepository.save(ScanJob.builder(scanJob)
                .status(Status.STARTED)
                .logMessage(logStarted)
                .build());

        ScanResult result = null;
        LogMessage logMessage;
        try {
            result = scanner.scan(targetFolders);
            logMessage = logService.info(log, "scanJobService.scanJobComplete", ImmutableList.of(targetPaths.toString()),
                    String.format("Scan job complete for '%s'.", targetPaths));
        } catch (IOException e) {
            logMessage = logService.error(log, "scanJobService.scanJobFailed.IOException",
                    "Scan job failed due to I/O error.", e);
        } catch (ConcurrentScanException e) {
            logMessage = logService.error(log, "scanJobService.scanJobFailed.ConcurrentScanException", 
                    "Library is already being scanned.");
        } catch (Exception e) {
            logMessage = logService.error(log, "scanJobService.scanJobFailed", 
                    "Unexpected error occurred when performing scan job.", e);
        }
        
        scanJobRepository.save(ScanJob.builder(scanJob)
                .status(result != null ? Status.COMPLETE : Status.FAILED)
                .scanResult(result)
                .logMessage(logMessage)
                .build());
    }

    private void doEditJob(ScanJob scanJob, List<EditCommand> commands) {

        LogMessage logStarted = logService.info(log, "scanJobService.editJobStarted", ImmutableList.of(String.valueOf(commands.size())),
                String.format("Started edit job for %d songs...", commands.size()));
        scanJobRepository.save(ScanJob.builder(scanJob)
                .status(Status.STARTED)
                .logMessage(logStarted)
                .build());

        ScanResult result = null;
        LogMessage logMessage;
        try {
            result = scanner.edit(commands);
            logMessage = logService.info(log, "scanJobService.editJobComplete", ImmutableList.of(String.valueOf(commands.size())),
                    String.format("Edit job complete for %d songs.", commands.size()));
        } catch (SongNotFoundException e) {
            logMessage = logService.error(log, "scanJobService.editJobFailed.SongNotFoundException", ImmutableList.of(String.valueOf(e.getId())), 
                    String.format("Song '%d' not found.", e.getId()));
        } catch (IOException e) {
            logMessage = logService.error(log, "scanJobService.editJobFailed.IOException",
                    "Edit job failed due to I/O error.", e);
        } catch (ConcurrentScanException e) {
            logMessage = logService.error(log, "scanJobService.editJobFailed.ConcurrentScanException", 
                    "Library is already scanning.");
        } catch (Exception e) {
            logMessage = logService.error(log, "scanJobService.editJobFailed", 
                    "Unexpected error occurred when performing edit job.", e);
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
                log.trace("Too early for automatic scan.");
            }
        } else {
            log.trace("Library was never scanned before.");
            return true;
        }
        return false;
    }
}
