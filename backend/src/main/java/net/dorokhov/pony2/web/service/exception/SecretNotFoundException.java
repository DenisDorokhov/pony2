package net.dorokhov.pony2.web.service.exception;

public class SecretNotFoundException extends Exception {
    public SecretNotFoundException() {
        super("Secret not found.");
    }
}
