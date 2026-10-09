package net.dorokhov.pony2.api.library.service.exception;

public class ConcurrentLibraryJobException extends Exception {
    public ConcurrentLibraryJobException() {
        super("Library job is already running.");
    }
}
