package net.dorokhov.pony.web.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public class FrontendLogCommandDto {

    public enum Level {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    private final LocalDateTime date;
    private final Level level;
    private final String message;

    @JsonCreator
    public FrontendLogCommandDto(LocalDateTime date, Level level, String message) {
        this.date = date;
        this.level = checkNotNull(level);
        this.message = checkNotNull(message);
    }

    public FrontendLogCommandDto(Level level, String message) {
        this(LocalDateTime.now(), level, message);
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }
}
