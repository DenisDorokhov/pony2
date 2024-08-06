package net.dorokhov.pony2.core.library.service.scan;

public interface ProgressObserver {
    void onProgress(long itemsComplete, long itemsTotal);
}
