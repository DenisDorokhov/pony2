package net.dorokhov.pony.installation.service.exception;

public final class NotInstalledException extends Exception {

    public NotInstalledException() {
        super("Not installed.");
    }
}
