package net.dorokhov.pony.library.service.impl.scan;

interface ProgressObserver {
    void onProgress(int itemsComplete, int itemsTotal);
}
