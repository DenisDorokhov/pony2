package net.dorokhov.pony.library.service.exception;

public final class LibraryNotDefinedException extends RuntimeException {

    public LibraryNotDefinedException() {
        super("Library not defined.");
    }
}
