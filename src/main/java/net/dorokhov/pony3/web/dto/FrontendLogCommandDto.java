package net.dorokhov.pony3.web.dto;

import java.time.LocalDateTime;

public class FrontendLogCommandDto {

    public enum Level {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    private LocalDateTime date;
    private Level level;
    private String message;

    public LocalDateTime getDate() {
        return date;
    }

    public FrontendLogCommandDto setDate(LocalDateTime date) {
        this.date = date;
        return this;
    }

    public Level getLevel() {
        return level;
    }

    public FrontendLogCommandDto setLevel(Level level) {
        this.level = level;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public FrontendLogCommandDto setMessage(String message) {
        this.message = message;
        return this;
    }
}
