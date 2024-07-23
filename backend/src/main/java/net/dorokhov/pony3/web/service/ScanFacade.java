package net.dorokhov.pony3.web.service;

import net.dorokhov.pony3.api.library.domain.ScanJob;
import net.dorokhov.pony3.api.library.domain.ScanJobProgress;
import net.dorokhov.pony3.api.library.domain.ScanResult;
import net.dorokhov.pony3.api.library.service.ScanJobService;
import net.dorokhov.pony3.api.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony3.web.dto.*;
import net.dorokhov.pony3.web.service.exception.ObjectNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.data.domain.Sort.Direction.DESC;

@Service
public class ScanFacade {

    private static final int PAGE_SIZE = 30;
    
    private final ScanJobService scanJobService;

    public ScanFacade(ScanJobService scanJobService) {
        this.scanJobService = scanJobService;
    }

    @Transactional(readOnly = true)
    public ScanStatusDto getScanStatus() {
        ScanJobProgress scanJobProgress = scanJobService.getCurrentScanJobProgress().orElse(null);
        return new ScanStatusDto().setScanning(scanJobProgress != null);
    }

    @Transactional(readOnly = true)
    public ScanStatisticsDto getScanStatistics() throws ObjectNotFoundException {
        return scanJobService.getLastSuccessfulJob()
                .map(ScanJob::getScanResult)
                .map(ScanStatisticsDto::of)
                .orElseThrow(() -> new ObjectNotFoundException(ScanResult.class));
    }

    @Transactional(readOnly = true)
    public ScanJobProgressDto getCurrentScanJobProgress() throws ObjectNotFoundException {
        ScanJobProgress scanJobProgress = scanJobService.getCurrentScanJobProgress().orElse(null);
        if (scanJobProgress == null) {
            throw new ObjectNotFoundException(ScanJobProgress.class);
        }
        return ScanJobProgressDto.of(scanJobProgress);
    }

    @Transactional(readOnly = true)
    public ScanJobProgressDto getScanJobProgress(String scanJobId) throws ObjectNotFoundException {
        ScanJobProgress scanJobProgress = scanJobService.getScanJobProgress(scanJobId).orElse(null);
        if (scanJobProgress == null) {
            throw new ObjectNotFoundException(ScanJob.class, scanJobId);
        }
        return ScanJobProgressDto.of(scanJobProgress);
    }

    @Transactional(readOnly = true)
    public ScanJobPageDto getScanJobs(int pageIndex, int pageSize) {
        return ScanJobPageDto.of(scanJobService.getAll(PageRequest.of(pageIndex, Math.min(PAGE_SIZE, Math.abs(pageSize)),
                Sort.by(DESC, "creationDate", "updateDate"))));
    }

    @Transactional(readOnly = true)
    public ScanJobDto getScanJob(String scanJobId) throws ObjectNotFoundException {
        ScanJob scanJob = scanJobService.getById(scanJobId).orElse(null);
        if (scanJob == null) {
            throw new ObjectNotFoundException(ScanJob.class, scanJobId);
        }
        return ScanJobDto.of(scanJob);
    }

    @Transactional
    public ScanJobDto startScanJob() throws ConcurrentScanException {
        return ScanJobDto.of(scanJobService.startScanJob());
    }
}
