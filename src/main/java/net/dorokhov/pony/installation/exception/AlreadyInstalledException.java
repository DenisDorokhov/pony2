package net.dorokhov.pony.installation.exception;

public class AlreadyInstalledException extends RuntimeException {

    public AlreadyInstalledException() {
        super("Already installed.");
    }
}
