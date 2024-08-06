package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.installation.domain.Installation;

import java.time.LocalDateTime;

public final class InstallationDto {

    private LocalDateTime creationDate;
    private LocalDateTime updateDate;
    private String version;

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public InstallationDto setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    @Nullable
    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public InstallationDto setUpdateDate(@Nullable LocalDateTime updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public InstallationDto setVersion(String version) {
        this.version = version;
        return this;
    }

    public static InstallationDto of(Installation installation) {
        return new InstallationDto()
                .setCreationDate(installation.getCreationDate())
                .setUpdateDate(installation.getUpdateDate())
                .setVersion(installation.getVersion());
    }
}
