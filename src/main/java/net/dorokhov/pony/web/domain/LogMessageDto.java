package net.dorokhov.pony.web.domain;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.log.domain.LogMessage;

import java.time.LocalDateTime;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class LogMessageDto {
    
    public enum Level {
        
        DEBUG, INFO, WARN, ERROR;
        
        public static Level of(LogMessage.Level level) {
            switch (level) {
                case DEBUG:
                    return DEBUG;
                case INFO:
                    return INFO;
                case WARN:
                    return WARN;
                case ERROR:
                    return ERROR;
                default:
                    throw new IllegalArgumentException(String.format("Unknown level value '%s'.", level));
            }
        }
        
        public LogMessage.Level convert() {
            switch (this) {
                case DEBUG:
                    return LogMessage.Level.DEBUG;
                case INFO:
                    return LogMessage.Level.INFO;
                case WARN:
                    return LogMessage.Level.WARN;
                case ERROR:
                    return LogMessage.Level.ERROR;
                default:
                    throw new IllegalArgumentException(String.format("Unknown level value '%s'.", this));
            }
        }
    }
    
    private final Long id;
    private final LocalDateTime date;
    private final Level level;
    private final String pattern;
    private final List<String> arguments;
    private final String text;

    public LogMessageDto(Long id, LocalDateTime date, Level level, String pattern, List<String> arguments, String text) {
        this.id = checkNotNull(id);
        this.date = checkNotNull(date);
        this.level = checkNotNull(level);
        this.pattern = checkNotNull(pattern);
        this.arguments = ImmutableList.copyOf(arguments);
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
                Level.of(logMessage.getLevel()),
                logMessage.getPattern(), 
                logMessage.getArguments(), 
                logMessage.getText());
    }
}
