package net.dorokhov.pony3.web.dto;

import net.dorokhov.pony3.web.validation.FolderExists;

import java.io.File;

public final class LibraryFolderDto {

    @FolderExists
    private String path;

    public String getPath() {
        return path;
    }

    public LibraryFolderDto setPath(String path) {
        this.path = path;
        return this;
    }

    public static LibraryFolderDto of(File file) {
        return new LibraryFolderDto().setPath(file.getAbsolutePath());
    }
}
