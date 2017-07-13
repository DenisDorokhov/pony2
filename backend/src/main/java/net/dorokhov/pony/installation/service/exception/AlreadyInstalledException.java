package net.dorokhov.pony.installation.service.exception;

public final class AlreadyInstalledException extends Exception {

    public AlreadyInstalledException() {
        super("Already installed.");
    }
}
