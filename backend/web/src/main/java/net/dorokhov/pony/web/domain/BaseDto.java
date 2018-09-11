package net.dorokhov.pony.web.domain;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public class BaseDto {

    protected final String id;
    protected final LocalDateTime creationDate;
    protected final LocalDateTime updateDate;

    BaseDto(String id, LocalDateTime creationDate, @Nullable LocalDateTime updateDate) {
        this.id = checkNotNull(id);
        this.creationDate = checkNotNull(creationDate);
        this.updateDate = updateDate;
    }

    public final String getId() {
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
