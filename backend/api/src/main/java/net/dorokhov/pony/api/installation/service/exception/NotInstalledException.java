package net.dorokhov.pony.api.installation.service.exception;

public final class NotInstalledException extends Exception {

    public NotInstalledException() {
        super("Not installed.");
    }
}
