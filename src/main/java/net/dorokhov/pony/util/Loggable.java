package net.dorokhov.pony.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Loggable {
    
    default Logger log() {
        return LoggerFactory.getLogger(getClass());
    }
}
