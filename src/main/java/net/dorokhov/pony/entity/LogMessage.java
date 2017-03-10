package net.dorokhov.pony.entity;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import net.dorokhov.pony.util.JsonAttributeConverter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "log_message")
public class LogMessage {

    public static enum Type {
        DEBUG, INFO, WARN, ERROR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Column(name = "type", nullable = false)
    private Type type;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "text")
    private String text;

    @Column(name = "details")
    private String details;

    @Column(name = "arguments")
    @Convert(converter = JsonAttributeConverter.ListConverter.class)
    private List<Object> arguments = new ArrayList<>();

    private LogMessage() {
    }

    public LogMessage(Type type, String code) {
        setType(type);
        setCode(code);
    }

    public Optional<Long> getId() {
        return Optional.ofNullable(id);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Optional<LocalDateTime> getDate() {
        return Optional.ofNullable(date);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = Preconditions.checkNotNull(type);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = Preconditions.checkNotNull(code);
    }

    public Optional<String> getText() {
        return Optional.ofNullable(text);
    }

    public void setText(String text) {
        this.text = text;
    }

    public Optional<String> getDetails() {
        return Optional.ofNullable(details);
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public List<Object> getArguments() {
        return arguments;
    }

    public void setArguments(List<Object> arguments) {
        this.arguments = Preconditions.checkNotNull(arguments);
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
                .add("date", date)
                .add("type", type)
                .add("code", code)
                .add("text", text)
                .add("details", details)
                .add("arguments", arguments)
                .toString();
    }
}
