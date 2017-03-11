package net.dorokhov.pony.installation;

public class AlreadyInstalledException extends RuntimeException {

    public AlreadyInstalledException() {
        super("Already installed.");
    }
}
