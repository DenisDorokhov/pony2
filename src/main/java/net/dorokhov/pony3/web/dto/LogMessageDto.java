package net.dorokhov.pony3.web.dto;

import net.dorokhov.pony3.api.log.domain.LogMessage;
import net.dorokhov.pony3.api.log.domain.LogMessage.Level;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class LogMessageDto {

    private String id;
    private LocalDateTime date;
    private Level level;
    private String pattern;
    private List<String> arguments = new ArrayList<>();
    private String text;

    public String getId() {
        return id;
    }

    public LogMessageDto setId(String id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public LogMessageDto setDate(LocalDateTime date) {
        this.date = date;
        return this;
    }

    public Level getLevel() {
        return level;
    }

    public LogMessageDto setLevel(Level level) {
        this.level = level;
        return this;
    }

    public String getPattern() {
        return pattern;
    }

    public LogMessageDto setPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public List<String> getArguments() {
        if (arguments == null) {
            arguments = new ArrayList<>();
        }
        return arguments;
    }

    public LogMessageDto setArguments(List<String> arguments) {
        this.arguments = arguments;
        return this;
    }

    public String getText() {
        return text;
    }

    public LogMessageDto setText(String text) {
        this.text = text;
        return this;
    }

    public static LogMessageDto of(LogMessage logMessage) {
        return new LogMessageDto()
                .setId(logMessage.getId())
                .setDate(logMessage.getDate())
                .setLevel(logMessage.getLevel())
                .setPattern(logMessage.getPattern())
                .setArguments(logMessage.getArguments())
                .setText(logMessage.getText());
    }
}
