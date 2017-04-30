package net.dorokhov.pony.installation.service.exception;

public class AlreadyInstalledException extends RuntimeException {

    public AlreadyInstalledException() {
        super("Already installed.");
    }
}
