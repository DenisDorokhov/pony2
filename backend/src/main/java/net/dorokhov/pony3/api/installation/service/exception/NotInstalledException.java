package net.dorokhov.pony3.api.installation.service.exception;

public final class NotInstalledException extends Exception {
    public NotInstalledException() {
        super("Not installed.");
    }
}
