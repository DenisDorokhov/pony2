package net.dorokhov.pony.library.service.exception;

public class NoScanEditCommandException extends RuntimeException {

    public NoScanEditCommandException() {
        super("No scan edit command defined.");
    }
}
