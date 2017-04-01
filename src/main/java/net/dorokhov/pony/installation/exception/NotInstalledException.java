package net.dorokhov.pony.installation.exception;

public class NotInstalledException extends RuntimeException {

    public NotInstalledException() {
        super("Not installed.");
    }
}
