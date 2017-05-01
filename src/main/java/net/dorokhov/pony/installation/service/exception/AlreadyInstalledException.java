package net.dorokhov.pony.installation.service.exception;

public final class AlreadyInstalledException extends RuntimeException {

    public AlreadyInstalledException() {
        super("Already installed.");
    }
}
