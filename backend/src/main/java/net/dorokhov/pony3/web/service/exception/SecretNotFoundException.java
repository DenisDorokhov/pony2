package net.dorokhov.pony3.web.service.exception;

public class SecretNotFoundException extends Exception {
    public SecretNotFoundException() {
        super("Secret not found.");
    }
}
