package net.dorokhov.pony.library.service.impl.scan;

public interface ProgressObserver {
    void onProgress(long itemsComplete, long itemsTotal);
}
