package net.dorokhov.pony.api.library.service;

import net.dorokhov.pony.api.library.domain.ScanJob;
import net.dorokhov.pony.api.library.domain.ScanJobProgress;
import net.dorokhov.pony.api.library.service.command.EditCommand;
import net.dorokhov.pony.api.library.service.exception.ConcurrentScanException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nullable;
import java.util.List;

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

    @Nullable
    ScanJobProgress getCurrentScanJobProgress();
    
    @Nullable
    ScanJobProgress getScanJobProgress(String id);

    Page<ScanJob> getAll(Pageable pageable);

    ScanJob getById(String id);
    
    @Nullable
    ScanJob getLastSuccessfulJob();
    
    ScanJob startScanJob() throws ConcurrentScanException;
    ScanJob startEditJob(List<EditCommand> commands) throws ConcurrentScanException;
}
