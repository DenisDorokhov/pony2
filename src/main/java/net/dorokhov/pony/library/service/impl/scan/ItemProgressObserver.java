package net.dorokhov.pony.library.service.impl.scan;

interface ItemProgressObserver {
    void onProgress(int itemsComplete, int itemsTotal);
}
