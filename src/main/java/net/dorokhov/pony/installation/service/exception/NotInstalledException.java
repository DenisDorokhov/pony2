package net.dorokhov.pony.installation.service.exception;

public class NotInstalledException extends RuntimeException {

    public NotInstalledException() {
        super("Not installed.");
    }
}
