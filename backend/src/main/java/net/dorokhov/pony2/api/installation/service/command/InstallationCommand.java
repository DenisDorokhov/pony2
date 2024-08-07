package net.dorokhov.pony2.api.installation.service.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class InstallationCommand {

    private List<File> libraryFolders = new ArrayList<>();
    private String adminName;
    private String adminEmail;
    private String adminPassword;
    private boolean startScanJobAfterInstallation;

    public List<File> getLibraryFolders() {
        if (libraryFolders == null) {
            libraryFolders = new ArrayList<>();
        }
        return libraryFolders;
    }

    public InstallationCommand setLibraryFolders(List<File> libraryFolders) {
        this.libraryFolders = new ArrayList<>(libraryFolders);
        return this;
    }

    public String getAdminName() {
        return adminName;
    }

    public InstallationCommand setAdminName(String adminName) {
        this.adminName = adminName;
        return this;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public InstallationCommand setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
        return this;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public InstallationCommand setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
        return this;
    }

    public boolean isStartScanJobAfterInstallation() {
        return startScanJobAfterInstallation;
    }

    public InstallationCommand setStartScanJobAfterInstallation(boolean startScanJobAfterInstallation) {
        this.startScanJobAfterInstallation = startScanJobAfterInstallation;
        return this;
    }
}
