package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.web.validation.FolderExists;

import java.io.File;

public final class LibraryFolderDto {

    @FolderExists
    private final String path;

    LibraryFolderDto(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public File convert() {
        return new File(path);
    }

    public static LibraryFolderDto of(File file) {
        return new LibraryFolderDto(file.getAbsolutePath());
    }
}
