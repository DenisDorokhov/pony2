package net.dorokhov.pony.web.service.exception;

public class InvalidInstallationSecretException extends Exception {

    public InvalidInstallationSecretException() {
        super("Installation secret in invalid.");
    }
}
