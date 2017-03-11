package net.dorokhov.pony.installation;

public class NotInstalledException extends RuntimeException {

    public NotInstalledException() {
        super("Not installed.");
    }
}
