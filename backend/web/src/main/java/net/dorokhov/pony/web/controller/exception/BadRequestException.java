package net.dorokhov.pony.web.controller.exception;

public final class BadRequestException extends Exception {

    public BadRequestException() {
        super("Bad request.");
    }
}
