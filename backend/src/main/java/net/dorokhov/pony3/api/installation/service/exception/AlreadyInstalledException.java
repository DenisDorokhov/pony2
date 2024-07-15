package net.dorokhov.pony3.api.installation.service.exception;

public final class AlreadyInstalledException extends Exception {
    public AlreadyInstalledException() {
        super("Already installed.");
    }
}
