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

@Entity
@Table(name = "log_message")
public class LogMessage implements Serializable {

    public enum Type {
        DEBUG, INFO, WARN, ERROR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
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
    @NotNull
    private String text;

    @Column(name = "details")
    private String details;

    @Column(name = "arguments")
    @Convert(converter = JsonAttributeConverter.class)
    @NotNull
    private List<String> arguments = ImmutableList.of();

    protected LogMessage() {
    }

    private LogMessage(Builder builder) {
        id = builder.id;
        date = builder.date;
        type = checkNotNull(builder.type);
        code = checkNotNull(builder.code);
        text = checkNotNull(builder.text);
        details = builder.details;
        arguments = builder.arguments.build();
    }

    @Nullable
    public Long getId() {
        return id;
    }

    @Nullable
    public LocalDateTime getDate() {
        return date;
    }

    public Type getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    @Nullable
    public String getDetails() {
        return details;
    }

    public List<String> getArguments() {
        return arguments != null ? arguments : ImmutableList.of();
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
                ", type=" + type +
                ", code='" + code + '\'' +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        
        private Long id;
        private LocalDateTime date;
        private Type type;
        private String code;
        private String text;
        private String details;
        private ImmutableList.Builder<String> arguments = ImmutableList.builder();

        public Builder() {
        }

        public Builder id(@Nullable Long id) {
            this.id = id;
            return this;
        }

        public Builder date(@Nullable LocalDateTime date) {
            this.date = date;
            return this;
        }

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder details(@Nullable String details) {
            this.details = details;
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

        public Builder addArguments(String... arguments) {
            this.arguments.addAll(Arrays.asList(arguments));
            return this;
        }

        public LogMessage build() {
            return new LogMessage(this);
        }
    }
}
