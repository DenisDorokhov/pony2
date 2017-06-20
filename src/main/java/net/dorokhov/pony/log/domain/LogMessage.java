package net.dorokhov.pony.log.domain;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.dorokhov.pony.common.JsonAttributeConverter;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;

@Entity
@Table(name = "log_message")
public class LogMessage implements Serializable {

    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    private Long id;

    @Column(name = "date", nullable = false, updatable = false)
    @NotNull
    private LocalDateTime date;

    @Column(name = "level", nullable = false)
    @NotNull
    private Level level;
    
    @Column(name = "pattern", nullable = false)
    @NotNull
    private String pattern;

    @Column(name = "arguments")
    @Convert(converter = JsonAttributeConverter.class)
    @NotNull
    private List<String> arguments = emptyList();

    @Column(name = "text")
    @NotNull
    private String text;

    protected LogMessage() {
    }

    private LogMessage(Builder builder) {
        id = builder.id;
        date = builder.date;
        level = checkNotNull(builder.level);
        pattern = checkNotNull(builder.pattern);
        arguments = builder.arguments.build();
        text = checkNotNull(builder.text);
    }

    @Nullable
    public Long getId() {
        return id;
    }

    @Nullable
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
        return arguments != null ? arguments : emptyList();
    }

    public String getText() {
        return text;
    }

    @PrePersist
    public void prePersist() {
        date = LocalDateTime.now();
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : super.hashCode();
    }

    @Override
    @SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && id != null && getClass().equals(obj.getClass())) {
            LogMessage that = (LogMessage) obj;
            return id.equals(that.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return "LogMessage{" +
                "id=" + id +
                ", level=" + level +
                ", text='" + text + '\'' +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        
        private Long id;
        private LocalDateTime date;
        private Level level;
        private String pattern;
        private ImmutableList.Builder<String> arguments = ImmutableList.builder();
        private String text;

        private Builder() {
        }

        public Builder id(@Nullable Long id) {
            this.id = id;
            return this;
        }

        public Builder date(@Nullable LocalDateTime date) {
            this.date = date;
            return this;
        }

        public Builder type(Level level) {
            this.level = level;
            return this;
        }

        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder arguments(@Nullable List<String> arguments) {
            if (arguments != null) {
                this.arguments = ImmutableList.<String>builder().addAll(arguments);
            } else {
                this.arguments = ImmutableList.builder();
            }
            return this;
        }

        public Builder arguments(String... arguments) {
            return arguments(Arrays.asList(arguments));
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public LogMessage build() {
            return new LogMessage(this);
        }
    }
}
