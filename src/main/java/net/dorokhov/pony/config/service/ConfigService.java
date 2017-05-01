package net.dorokhov.pony.config.service;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

public interface ConfigService {

    @Nullable
    Integer getAutoScanInterval();

    void saveAutoScanInterval(@Nullable Integer value);

    List<File> fetchLibraryFolders();

    void saveLibraryFolders(List<File> libraryFolders);
}
