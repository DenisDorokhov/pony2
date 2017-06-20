package net.dorokhov.pony.web.domain;

public final class InstallationStatusDto {

    private final boolean installed;

    public InstallationStatusDto(boolean installed) {
        this.installed = installed;
    }

    public boolean isInstalled() {
        return installed;
    }
}
