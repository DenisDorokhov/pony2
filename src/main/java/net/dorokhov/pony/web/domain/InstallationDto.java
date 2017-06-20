package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.installation.domain.Installation;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public final class InstallationDto {

    private final LocalDateTime creationDate;
    private final LocalDateTime updateDate;

    private final String version;

    InstallationDto(LocalDateTime creationDate, @Nullable LocalDateTime updateDate, String version) {
        this.creationDate = checkNotNull(creationDate);
        this.updateDate = updateDate;
        this.version = checkNotNull(version);
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    @Nullable
    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public String getVersion() {
        return version;
    }

    public static InstallationDto of(Installation installation) {
        return new InstallationDto(installation.getCreationDate(), installation.getUpdateDate(), installation.getVersion());
    }
}
