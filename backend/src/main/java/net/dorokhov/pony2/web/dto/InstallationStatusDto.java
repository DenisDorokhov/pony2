package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public final class InstallationStatusDto {

    private boolean installed;
    private LocalDateTime initialScanDate;

    public boolean isInstalled() {
        return installed;
    }

    public InstallationStatusDto setInstalled(boolean installed) {
        this.installed = installed;
        return this;
    }

    @Nullable
    public LocalDateTime getInitialScanDate() {
        return initialScanDate;
    }

    public InstallationStatusDto setInitialScanDate(LocalDateTime initialScanDate) {
        this.initialScanDate = initialScanDate;
        return this;
    }
}
