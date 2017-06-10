package net.dorokhov.pony.common;

public class SecretNotFoundException extends Exception {
    public SecretNotFoundException() {
        super("Secret not found.");
    }
}
