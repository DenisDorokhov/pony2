package net.dorokhov.pony2.web.dto;

public final class ScanStatusDto {

    private boolean scanning;

    public boolean isScanning() {
        return scanning;
    }

    public ScanStatusDto setScanning(boolean scanning) {
        this.scanning = scanning;
        return this;
    }
}
