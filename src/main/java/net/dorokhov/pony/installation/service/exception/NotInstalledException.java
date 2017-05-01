package net.dorokhov.pony.installation.service.exception;

public final class NotInstalledException extends RuntimeException {

    public NotInstalledException() {
        super("Not installed.");
    }
}
