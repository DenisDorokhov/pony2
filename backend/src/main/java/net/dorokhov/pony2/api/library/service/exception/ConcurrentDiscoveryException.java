package net.dorokhov.pony2.api.library.service.exception;

public class ConcurrentDiscoveryException extends Exception {
    public ConcurrentDiscoveryException() {
        super("Discovery job is already running.");
    }
}
