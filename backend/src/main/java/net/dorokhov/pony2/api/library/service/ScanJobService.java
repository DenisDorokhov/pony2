package net.dorokhov.pony2.api.library.service;

import net.dorokhov.pony2.api.library.domain.ScanJob;
import net.dorokhov.pony2.api.library.domain.ScanJobProgress;
import net.dorokhov.pony2.api.library.service.command.EditCommand;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentScanException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ScanJobService {
    
    interface Observer {
        void onScanJobStarting(ScanJob scanJob);
        void onScanJobStarted(ScanJob scanJob);
        void onScanJobProgress(ScanJobProgress scanJobProgress);
        void onScanJobCompleting(ScanJob scanJob);
        void onScanJobCompleted(ScanJob scanJob);
        void onScanJobFailing(ScanJob scanJob);
        void onScanJobFailed(ScanJob scanJob);
    }
    
    void addObserver(Observer observer);
    void removeObserver(Observer observer);

    Optional<ScanJobProgress> getCurrentScanJobProgress();
    
    Optional<ScanJobProgress> getScanJobProgress(String id);

    Page<ScanJob> getAll(Pageable pageable);

    Optional<ScanJob> getById(String id);
    
    Optional<ScanJob> getFirstSuccessfulJob();
    Optional<ScanJob> getLastSuccessfulJob();

    ScanJob startScanJob() throws ConcurrentScanException;
    ScanJob startEditJob(List<EditCommand> commands) throws ConcurrentScanException;
}
