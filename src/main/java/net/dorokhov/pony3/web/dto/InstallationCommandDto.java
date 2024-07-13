package net.dorokhov.pony3.web.dto;

import net.dorokhov.pony3.api.installation.service.command.InstallationCommand;
import net.dorokhov.pony3.web.validation.InstallationSecret;
import net.dorokhov.pony3.web.validation.RepeatPassword;
import net.dorokhov.pony3.web.validation.RepeatPasswordValue;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RepeatPassword
public final class InstallationCommandDto {

    @InstallationSecret
    private String installationSecret;

    @NotNull
    @Valid
    private List<LibraryFolderDto> libraryFolders = new ArrayList<>();

    @NotBlank
    @Size(max = 255)
    private String adminName;

    @NotBlank
    @Email
    @Size(max = 255)
    private String adminEmail;

    @NotBlank
    @Size(min = 6, max = 255)
    @RepeatPasswordValue
    private String adminPassword;

    @RepeatPasswordValue(constraintViolationField = true)
    private String repeatAdminPassword;

    public String getInstallationSecret() {
        return installationSecret;
    }

    public InstallationCommandDto setInstallationSecret(String installationSecret) {
        this.installationSecret = installationSecret;
        return this;
    }

    public List<LibraryFolderDto> getLibraryFolders() {
        if (libraryFolders == null) {
            libraryFolders = new ArrayList<>();
        }
        return libraryFolders;
    }

    public InstallationCommandDto setLibraryFolders(List<LibraryFolderDto> libraryFolders) {
        this.libraryFolders = libraryFolders;
        return this;
    }

    public String getAdminName() {
        return adminName;
    }

    public InstallationCommandDto setAdminName(String adminName) {
        this.adminName = adminName;
        return this;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public InstallationCommandDto setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
        return this;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public InstallationCommandDto setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
        return this;
    }

    public String getRepeatAdminPassword() {
        return repeatAdminPassword;
    }

    public InstallationCommandDto setRepeatAdminPassword(String repeatAdminPassword) {
        this.repeatAdminPassword = repeatAdminPassword;
        return this;
    }

    public InstallationCommand convert() {
        return new InstallationCommand()
                .setLibraryFolders(libraryFolders.stream()
                        .map(folder -> new File(folder.getPath()))
                        .collect(Collectors.toList()))
                .setAdminName(adminName)
                .setAdminEmail(adminEmail)
                .setAdminPassword(adminPassword);
    }
}
