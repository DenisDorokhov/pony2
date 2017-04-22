package net.dorokhov.pony.installation;

import net.dorokhov.pony.entity.User;

import java.io.File;
import java.util.List;

public class InstallationDraft {

    private Integer autoScanInterval;

    private List<File> libraryFolders;

    private List<User> users;

    public InstallationDraft(Integer autoScanInterval, List<File> libraryFolders, List<User> users) {
        this.autoScanInterval = autoScanInterval;
        this.libraryFolders = libraryFolders;
        this.users = users;
    }

    public Integer getAutoScanInterval() {
        return autoScanInterval;
    }

    public List<File> getLibraryFolders() {
        return libraryFolders;
    }

    public List<User> getUsers() {
        return users;
    }
}
