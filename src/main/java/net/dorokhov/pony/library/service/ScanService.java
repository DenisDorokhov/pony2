package net.dorokhov.pony.library.service;

import net.dorokhov.pony.library.domain.ScanResult;
import net.dorokhov.pony.library.domain.ScanStatus;
import net.dorokhov.pony.library.service.command.EditCommand;
import net.dorokhov.pony.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony.library.service.exception.SongNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ScanService {

    @Nullable
    ScanStatus getStatus();

    Page<ScanResult> getAll(Pageable aPageable);

    @Nullable
    ScanResult getById(Long aId);

    ScanResult scan(List<File> aTargetFolders) throws ConcurrentScanException, IOException;
    ScanResult edit(List<EditCommand> aCommands) throws ConcurrentScanException, SongNotFoundException, IOException;
}
