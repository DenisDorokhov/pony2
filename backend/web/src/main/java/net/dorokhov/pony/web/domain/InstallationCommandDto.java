package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.installation.service.command.InstallationCommand;
import net.dorokhov.pony.web.validation.InstallationSecret;
import net.dorokhov.pony.web.validation.RepeatPassword;
import net.dorokhov.pony.web.validation.RepeatPasswordValue;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.stream.Collectors;

@RepeatPassword
public final class InstallationCommandDto {

    @InstallationSecret
    private final String installationSecret;

    @NotNull
    @Valid
    private final List<LibraryFolderDto> libraryFolders;

    @NotBlank
    @Size(max = 255)
    private final String adminName;

    @NotBlank
    @Email
    @Size(max = 255)
    private final String adminEmail;

    @NotBlank
    @Size(min = 6, max = 255)
    @RepeatPasswordValue
    private final String adminPassword;

    @RepeatPasswordValue(constraintViolationField = true)
    private final String repeatAdminPassword;

    public InstallationCommandDto(String installationSecret,
                                  List<LibraryFolderDto> libraryFolders,
                                  String adminName, String adminEmail, 
                                  String adminPassword, String repeatAdminPassword) {
        this.installationSecret = installationSecret;
        this.libraryFolders = libraryFolders;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.repeatAdminPassword = repeatAdminPassword;
    }

    public String getInstallationSecret() {
        return installationSecret;
    }

    public List<LibraryFolderDto> getLibraryFolders() {
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

    public String getRepeatAdminPassword() {
        return repeatAdminPassword;
    }

    public InstallationCommand convert() {
        return InstallationCommand.builder()
                .libraryFolders(libraryFolders.stream()
                        .map(LibraryFolderDto::convert)
                        .collect(Collectors.toList()))
                .adminName(adminName)
                .adminEmail(adminEmail)
                .adminPassword(adminPassword)
                .build();
    }
}
