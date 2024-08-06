package net.dorokhov.pony2.web.service.exception;

public class InvalidInstallationSecretException extends Exception {
    public InvalidInstallationSecretException() {
        super("Installation secret in invalid.");
    }
}
