package net.dorokhov.pony3.api.library.service.exception;

public class ConcurrentScanException extends Exception {
    public ConcurrentScanException() {
        super("Scan job is already running.");
    }
}
