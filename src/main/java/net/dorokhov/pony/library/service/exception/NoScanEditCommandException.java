package net.dorokhov.pony.library.service.exception;

public class NoScanEditCommandException extends Exception {

    public NoScanEditCommandException() {
        super("No scan edit command defined.");
    }
}
