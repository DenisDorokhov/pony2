package net.dorokhov.pony2.api.installation.service.exception;

public final class NotInstalledException extends Exception {
    public NotInstalledException() {
        super("Not installed.");
    }
}
