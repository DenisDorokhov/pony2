package net.dorokhov.pony.library.service.impl.scan;

interface Observer {
    void onProgress(int itemsComplete, int itemsTotal);
}
