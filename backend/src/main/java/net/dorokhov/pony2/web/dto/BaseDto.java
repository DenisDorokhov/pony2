package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

@SuppressWarnings("unchecked")
public class BaseDto<T extends BaseDto<?>> {

    private String id;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;

    public final String getId() {
        return id;
    }

    public T setId(String id) {
        this.id = id;
        return (T) this;
    }

    public final LocalDateTime getCreationDate() {
        return creationDate;
    }

    public T setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        return (T) this;
    }

    @Nullable
    public final LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public T setUpdateDate(@Nullable LocalDateTime updateDate) {
        this.updateDate = updateDate;
        return (T) this;
    }
}
