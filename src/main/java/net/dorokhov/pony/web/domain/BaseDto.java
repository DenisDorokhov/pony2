package net.dorokhov.pony.web.domain;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public class BaseDto {
    
    protected final Long id;
    protected final LocalDateTime creationDate;
    protected final LocalDateTime updateDate;

    public BaseDto(Long id, LocalDateTime creationDate, LocalDateTime updateDate) {
        this.id = checkNotNull(id);
        this.creationDate = checkNotNull(creationDate);
        this.updateDate = updateDate;
    }

    public final Long getId() {
        return id;
    }

    public final LocalDateTime getCreationDate() {
        return creationDate;
    }

    @Nullable
    public final LocalDateTime getUpdateDate() {
        return updateDate;
    }
}
