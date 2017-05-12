package net.dorokhov.pony.library.service;

import net.dorokhov.pony.library.domain.ScanJob;
import net.dorokhov.pony.library.domain.ScanStatus;
import net.dorokhov.pony.library.service.command.EditCommand;
import net.dorokhov.pony.library.service.exception.LibraryNotDefinedException;
import net.dorokhov.pony.library.service.exception.NoScanEditCommandException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nullable;
import java.util.List;

public interface ScanJobService {

    Page<ScanJob> getAll(Pageable pageable);

    ScanJob getById(Long id);
    
    ScanStatus getScanStatus();

    ScanJob startScanJob() throws LibraryNotDefinedException;
    ScanJob startEditJob(List<EditCommand> commands) throws NoScanEditCommandException;

    @Nullable
    ScanJob startAutoScanJobIfNeeded();
}
