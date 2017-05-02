package net.dorokhov.pony.library.service.exception;

public class ConcurrentScanException extends RuntimeException {

    public ConcurrentScanException() {
        super("Scan job is already running.");
    }
}
