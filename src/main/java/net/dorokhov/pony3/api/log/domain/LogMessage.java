package net.dorokhov.pony3.api.log.domain;

import com.google.common.base.MoreObjects;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony3.common.ListOfStringsJsonAttributeConverter;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "log_message")
public class LogMessage implements Serializable {

    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    private String id;

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
    @Convert(converter = ListOfStringsJsonAttributeConverter.class)
    @NotNull
    private List<String> arguments = new ArrayList<>();

    @Column(name = "text")
    @NotNull
    private String text;

    public String getId() {
        return id;
    }

    public LogMessage setId(String id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public LogMessage setDate(LocalDateTime date) {
        this.date = date;
        return this;
    }

    public Level getLevel() {
        return level;
    }

    public LogMessage setLevel(Level level) {
        this.level = level;
        return this;
    }

    public String getPattern() {
        return pattern;
    }

    public LogMessage setPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public List<String> getArguments() {
        if (arguments == null) {
            arguments = new ArrayList<>();
        }
        return arguments;
    }

    public LogMessage setArguments(List<String> arguments) {
        this.arguments = arguments;
        return this;
    }

    public String getText() {
        return text;
    }

    public LogMessage setText(String text) {
        this.text = text;
        return this;
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
    public boolean equals(Object obj) {
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
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("level", level)
                .add("text", text)
                .toString();
    }
}
