package net.dorokhov.pony2.core.library.service.exception;

public class LlmHallucinationException extends RuntimeException {

    public LlmHallucinationException(String message) {
        super(message);
    }
}
