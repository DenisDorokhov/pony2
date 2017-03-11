package net.dorokhov.pony.entity;

import com.google.common.base.MoreObjects;
import net.dorokhov.pony.util.JsonAttributeConverter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
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
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    @NotNull
    private Long id;

    @Column(name = "date", nullable = false, updatable = false)
    @NotNull
    private LocalDateTime date;

    @Column(name = "type", nullable = false)
    @NotNull
    private Type type;
    
    @Column(name = "code", nullable = false)
    @NotNull
    private String code;

    @Column(name = "text")
    private String text;

    @Column(name = "details")
    private String details;

    @Column(name = "arguments")
    @Convert(converter = JsonAttributeConverter.ListConverter.class)
    private List<Object> arguments;

    public LogMessage(Type type, String code) {
        setType(type);
        setCode(code);
    }

    public Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
        if (arguments == null) {
            arguments = new ArrayList<>();
        }
        return arguments;
    }

    public void setArguments(List<Object> arguments) {
        this.arguments = arguments;
    }

    @PrePersist
    public void prePersist() {
        setDate(LocalDateTime.now());
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
                .add("type", type)
                .add("code", code)
                .toString();
    }
}
