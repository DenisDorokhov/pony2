package net.dorokhov.pony.config;

import java.io.File;
import java.util.List;
import java.util.Optional;

public interface ConfigService {

    Optional<Integer> getAutoScanInterval();

    void saveAutoScanInterval(Integer value);

    List<File> fetchLibraryFolders();

    void saveLibraryFolders(List<File> libraryFolders);
}
