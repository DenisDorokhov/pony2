package net.dorokhov.pony.web.service.exception;

public class SecretNotFoundException extends Exception {
    public SecretNotFoundException() {
        super("Secret not found.");
    }
}
