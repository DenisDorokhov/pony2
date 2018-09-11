package net.dorokhov.pony.web.service;

import net.dorokhov.pony.api.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony.web.domain.*;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;

public interface ScanFacade {

    ScanStatusDto getScanStatus();

    ScanStatisticsDto getScanStatistics() throws ObjectNotFoundException;

    ScanJobProgressDto getCurrentScanJobProgress() throws ObjectNotFoundException;
    
    ScanJobProgressDto getScanJobProgress(String scanJobId) throws ObjectNotFoundException;

    ScanJobPageDto getScanJobs(int pageIndex);
    
    ScanJobDto getScanJob(String scanJobId) throws ObjectNotFoundException;

    ScanJobDto startScanJob() throws ConcurrentScanException;
}
