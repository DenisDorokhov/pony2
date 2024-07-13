package net.dorokhov.pony3.core.library.service.scan;

public interface ProgressObserver {
    void onProgress(long itemsComplete, long itemsTotal);
}
