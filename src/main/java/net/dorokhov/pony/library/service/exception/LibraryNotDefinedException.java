package net.dorokhov.pony.library.service.exception;

public final class LibraryNotDefinedException extends Exception {

    public LibraryNotDefinedException() {
        super("Library not defined.");
    }
}
