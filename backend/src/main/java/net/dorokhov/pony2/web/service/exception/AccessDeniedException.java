package net.dorokhov.pony2.web.service.exception;

public class AccessDeniedException extends Exception {
    public AccessDeniedException() {
        super("Access denied.");
    }
}
