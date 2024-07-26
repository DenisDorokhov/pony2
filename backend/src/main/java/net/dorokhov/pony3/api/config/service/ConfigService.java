package net.dorokhov.pony3.api.config.service;

import java.io.File;
import java.util.List;

public interface ConfigService {

    List<File> getLibraryFolders();

    void saveLibraryFolders(List<File> libraryFolders);
}
