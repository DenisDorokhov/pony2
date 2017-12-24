package net.dorokhov.pony.api.config.service;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

public interface ConfigService {

    @Nullable
    Integer getAutoScanInterval();

    void saveAutoScanInterval(@Nullable Integer value);

    List<File> getLibraryFolders();

    void saveLibraryFolders(List<File> libraryFolders);
}
