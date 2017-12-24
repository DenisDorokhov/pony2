package net.dorokhov.pony.web.service;

import net.dorokhov.pony.api.library.domain.ScanJob;
import net.dorokhov.pony.api.library.domain.ScanJobProgress;
import net.dorokhov.pony.api.library.domain.ScanResult;
import net.dorokhov.pony.api.library.service.ScanJobService;
import net.dorokhov.pony.api.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony.web.domain.*;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.springframework.data.domain.Sort.Direction.DESC;

@Service
public class ScanFacadeImpl implements ScanFacade {

    private static final int PAGE_SIZE = 30;
    
    private final ScanJobService scanJobService;

    public ScanFacadeImpl(ScanJobService scanJobService) {
        this.scanJobService = scanJobService;
    }

    @Override
    @Transactional(readOnly = true)
    public ScanStatusDto getScanStatus() {
        ScanJobProgress scanJobProgress = scanJobService.getCurrentScanJobProgress();
        return new ScanStatusDto(scanJobProgress != null);
    }

    @Override
    @Transactional(readOnly = true)
    public ScanStatisticsDto getScanStatistics() throws ObjectNotFoundException {
        return Optional.ofNullable(scanJobService.getLastSuccessfulJob())
                .map(ScanJob::getScanResult)
                .map(ScanStatisticsDto::of)
                .orElseThrow(() -> new ObjectNotFoundException(ScanResult.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ScanJobProgressDto getCurrentScanJobProgress() throws ObjectNotFoundException {
        ScanJobProgress scanJobProgress = scanJobService.getCurrentScanJobProgress();
        if (scanJobProgress == null) {
            throw new ObjectNotFoundException(ScanJobProgress.class);
        }
        return ScanJobProgressDto.of(scanJobProgress);
    }

    @Override
    @Transactional(readOnly = true)
    public ScanJobProgressDto getScanJobProgress(Long scanJobId) throws ObjectNotFoundException {
        ScanJobProgress scanJobProgress = scanJobService.getScanJobProgress(scanJobId);
        if (scanJobProgress == null) {
            throw new ObjectNotFoundException(ScanJob.class, scanJobId);
        }
        return ScanJobProgressDto.of(scanJobProgress);
    }

    @Override
    @Transactional(readOnly = true)
    public ScanJobPageDto getScanJobs(int pageIndex) {
        return ScanJobPageDto.of(scanJobService.getAll(new PageRequest(pageIndex, PAGE_SIZE, 
                new Sort(DESC, "creationDate", "updateDate"))));
    }

    @Override
    @Transactional(readOnly = true)
    public ScanJobDto getScanJob(Long scanJobId) throws ObjectNotFoundException {
        ScanJob scanJob = scanJobService.getById(scanJobId);
        if (scanJob == null) {
            throw new ObjectNotFoundException(ScanJob.class, scanJobId);
        }
        return ScanJobDto.of(scanJob);
    }

    @Override
    @Transactional
    public ScanJobDto startScanJob() throws ConcurrentScanException {
        return ScanJobDto.of(scanJobService.startScanJob());
    }
}
