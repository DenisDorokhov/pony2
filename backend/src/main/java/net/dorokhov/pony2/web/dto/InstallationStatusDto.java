package net.dorokhov.pony2.web.dto;

public final class InstallationStatusDto {

    private boolean installed;

    public boolean isInstalled() {
        return installed;
    }

    public InstallationStatusDto setInstalled(boolean installed) {
        this.installed = installed;
        return this;
    }
}
