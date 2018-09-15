package net.dorokhov.pony.web.domain;

import static com.google.common.base.Preconditions.checkNotNull;

public class FrontendLogCommandDto {

    public enum Level {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
    
    private final Level level;
    private final String message;

    public FrontendLogCommandDto(Level level, String message) {
        this.level = checkNotNull(level);
        this.message = checkNotNull(message);
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }
}
