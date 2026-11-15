package net.dorokhov.pony2.core.library.service.exception;

public class LlmContextLimitException extends RuntimeException {

    public LlmContextLimitException(String message) {
        super(message);
    }
}
