package net.dorokhov.pony.library.service.impl;

import net.dorokhov.pony.library.domain.ScanResult;
import net.dorokhov.pony.library.domain.ScanStatus;
import net.dorokhov.pony.library.service.ScanService;
import net.dorokhov.pony.library.service.command.EditCommand;
import net.dorokhov.pony.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony.library.service.exception.SongNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class ScanServiceImpl implements ScanService {
    
    @Override
    @Nullable
    public ScanStatus getStatus() {
        // TODO: implement
        return null;
    }

    @Override
    public Page<ScanResult> getAll(Pageable aPageable) {
        // TODO: implement
        return null;
    }

    @Override
    @Nullable
    public ScanResult getById(Long aId) {
        // TODO: implement
        return null;
    }

    @Override
    public ScanResult scan(List<File> aTargetFolders) throws ConcurrentScanException, IOException {
        // TODO: implement
        return null;
    }

    @Override
    public ScanResult edit(List<EditCommand> aCommands) throws ConcurrentScanException, SongNotFoundException, IOException {
        // TODO: implement
        return null;
    }
}
