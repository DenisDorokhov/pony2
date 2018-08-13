package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.log.domain.LogMessage;
import net.dorokhov.pony.api.log.domain.LogMessage.Level;

import java.time.LocalDateTime;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableList;

public final class LogMessageDto {

    private final Long id;
    private final LocalDateTime date;
    private final Level level;
    private final String pattern;
    private final List<String> arguments;
    private final String text;

    private LogMessageDto(Long id, LocalDateTime date, Level level, String pattern, List<String> arguments, String text) {
        this.id = checkNotNull(id);
        this.date = checkNotNull(date);
        this.level = checkNotNull(level);
        this.pattern = checkNotNull(pattern);
        this.arguments = unmodifiableList(arguments);
        this.text = checkNotNull(text);
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Level getLevel() {
        return level;
    }

    public String getPattern() {
        return pattern;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public String getText() {
        return text;
    }

    public static LogMessageDto of(LogMessage logMessage) {
        return new LogMessageDto(
                logMessage.getId(),
                logMessage.getDate(),
                logMessage.getLevel(),
                logMessage.getPattern(),
                logMessage.getArguments(),
                logMessage.getText());
    }
}
