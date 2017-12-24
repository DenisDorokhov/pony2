package net.dorokhov.pony.api.installation.service.command;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class InstallationCommand {

    private final Integer autoScanInterval;
    private final List<File> libraryFolders;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;

    private InstallationCommand(Builder builder) {
        autoScanInterval = builder.autoScanInterval;
        libraryFolders = builder.libraryFolders.build();
        adminName = checkNotNull(builder.adminName);
        adminEmail = checkNotNull(builder.adminEmail);
        adminPassword = checkNotNull(builder.adminPassword);
    }

    @Nullable
    public Integer getAutoScanInterval() {
        return autoScanInterval;
    }

    public List<File> getLibraryFolders() {
        return libraryFolders;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public String getAdminPassword() {
        return adminPassword;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        
        private Integer autoScanInterval;
        private ImmutableList.Builder<File> libraryFolders = ImmutableList.builder();
        private String adminName;
        private String adminEmail;
        private String adminPassword;

        public Builder autoScanInterval(@Nullable Integer autoScanInterval) {
            this.autoScanInterval = autoScanInterval;
            return this;
        }

        public Builder libraryFolders(List<File> libraryFolders) {
            this.libraryFolders = ImmutableList.<File>builder().addAll(libraryFolders);
            return this;
        }

        public Builder roles(File... libraryFolders) {
            return libraryFolders(Arrays.asList(libraryFolders));
        }

        public Builder adminName(String adminName) {
            this.adminName = adminName;
            return this;
        }

        public Builder adminEmail(String adminEmail) {
            this.adminEmail = adminEmail;
            return this;
        }

        public Builder adminPassword(String adminPassword) {
            this.adminPassword = adminPassword;
            return this;
        }

        public InstallationCommand build() {
            return new InstallationCommand(this);
        }
    }
}
