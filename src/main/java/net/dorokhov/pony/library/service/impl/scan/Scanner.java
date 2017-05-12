package net.dorokhov.pony.library.service.impl.scan;

import net.dorokhov.pony.library.domain.ScanResult;
import net.dorokhov.pony.library.domain.ScanStatus;
import net.dorokhov.pony.library.service.command.EditCommand;
import net.dorokhov.pony.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony.library.service.exception.SongNotFoundException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Component
public class Scanner {
    
    public ScanStatus getStatus() {
        // TODO: implement
        return null;
    }

    public ScanResult scan(List<File> targetFolders) throws ConcurrentScanException, IOException {
        // TODO: implement
        return null;
    }

    public ScanResult edit(List<EditCommand> commands) throws ConcurrentScanException, SongNotFoundException, IOException {
        // TODO: implement
        return null;
    }
}
