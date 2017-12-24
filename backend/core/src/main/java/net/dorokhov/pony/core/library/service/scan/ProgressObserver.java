package net.dorokhov.pony.core.library.service.scan;

public interface ProgressObserver {
    void onProgress(long itemsComplete, long itemsTotal);
}
