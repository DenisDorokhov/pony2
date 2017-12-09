package net.dorokhov.pony.library.service.scan;

public interface ProgressObserver {
    void onProgress(long itemsComplete, long itemsTotal);
}
