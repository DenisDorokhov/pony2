package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.library.domain.ScanJob;
import net.dorokhov.pony2.api.library.domain.ScanJobProgress;
import net.dorokhov.pony2.api.library.service.ScanJobService;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony2.web.dto.*;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
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
    public OptionalResponseDto<ScanStatisticsDto> getScanStatistics() {
        return scanJobService.getLastSuccessfulJob()
                .map(ScanJob::getScanResult)
                .map(ScanStatisticsDto::of)
                .map(OptionalResponseDto::of)
                .orElseGet(OptionalResponseDto::empty);
    }

    @Transactional(readOnly = true)
    public OptionalResponseDto<ScanJobProgressDto> getCurrentScanJobProgress() {
        ScanJobProgress scanJobProgress = scanJobService.getCurrentScanJobProgress().orElse(null);
        if (scanJobProgress == null) {
            return OptionalResponseDto.empty();
        }
        return OptionalResponseDto.of(ScanJobProgressDto.of(scanJobProgress));
    }

    @Transactional(readOnly = true)
    public OptionalResponseDto<ScanJobProgressDto> getScanJobProgress(String scanJobId) {
        ScanJobProgress scanJobProgress = scanJobService.getScanJobProgress(scanJobId).orElse(null);
        if (scanJobProgress == null) {
            return OptionalResponseDto.empty();
        }
        return OptionalResponseDto.of(ScanJobProgressDto.of(scanJobProgress));
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
