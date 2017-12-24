package net.dorokhov.pony.api.library.service.exception;

public class ConcurrentScanException extends Exception {

    public ConcurrentScanException() {
        super("Scan job is already running.");
    }
}
