package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.installation.service.command.InstallationCommand;
import net.dorokhov.pony.web.validation.FolderExists;
import net.dorokhov.pony.web.validation.InstallationSecret;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public final class InstallationCommandDto {

    public static final class LibraryFolder {
        
        @FolderExists
        private final String path;

        public LibraryFolder(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }
    
    @InstallationSecret
    private final String installationSecret;

    @NotNull
    @Valid
    private final List<LibraryFolder> libraryFolders;

    @NotBlank
    @Size(max = 255)
    private final String adminName;

    @NotBlank
    @Email
    @Size(max = 255)
    private final String adminEmail;

    @NotBlank
    @Size(min = 6, max = 255)
    private final String adminPassword;

    public InstallationCommandDto(String installationSecret, 
                                  List<LibraryFolder> libraryFolders, 
                                  String adminName, String adminEmail, String adminPassword) {
        this.installationSecret = installationSecret;
        this.libraryFolders = libraryFolders;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    public String getInstallationSecret() {
        return installationSecret;
    }

    public List<LibraryFolder> getLibraryFolders() {
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

    public InstallationCommand convert() {
        return InstallationCommand.builder()
                .libraryFolders(libraryFolders.stream()
                        .map(libraryFolderDto -> libraryFolderDto.path)
                        .map(File::new)
                        .collect(Collectors.toList()))
                .adminName(adminName)
                .adminEmail(adminEmail)
                .adminPassword(adminPassword)
                .build();
    }
}
