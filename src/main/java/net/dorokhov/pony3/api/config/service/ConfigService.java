package net.dorokhov.pony3.api.config.service;

import jakarta.annotation.Nullable;

import java.io.File;
import java.util.List;
import java.util.Optional;

public interface ConfigService {

    Optional<Integer> getAutoScanInterval();

    void saveAutoScanInterval(@Nullable Integer value);

    List<File> getLibraryFolders();

    void saveLibraryFolders(List<File> libraryFolders);
}
