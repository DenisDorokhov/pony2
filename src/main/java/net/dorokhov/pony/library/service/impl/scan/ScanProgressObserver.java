package net.dorokhov.pony.library.service.impl.scan;

import net.dorokhov.pony.library.domain.ScanResult;
import net.dorokhov.pony.library.domain.ScanStatus;

public interface ScanProgressObserver {
    void onScanStarted(ScanStatus scanStatus);
    void onScanProgress(ScanStatus lastScanStatus, ScanStatus newScanStatus);
    void onScanFailed(ScanStatus lastScanStatus, Exception e);
    void onScanComplete(ScanStatus lastScanStatus, ScanResult scanResult);
}
