package net.dorokhov.pony3.web.controller.exception;

public final class BadRequestException extends Exception {
    public BadRequestException() {
        super("Bad request.");
    }
}
