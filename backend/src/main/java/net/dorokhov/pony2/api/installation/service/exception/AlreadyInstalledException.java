package net.dorokhov.pony2.api.installation.service.exception;

public final class AlreadyInstalledException extends Exception {
    public AlreadyInstalledException() {
        super("Already installed.");
    }
}
