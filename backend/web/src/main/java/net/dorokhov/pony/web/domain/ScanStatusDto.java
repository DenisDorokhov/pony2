package net.dorokhov.pony.web.domain;

public final class ScanStatusDto {

    private final boolean scanning;

    public ScanStatusDto(boolean scanning) {
        this.scanning = scanning;
    }

    public boolean isScanning() {
        return scanning;
    }
}
