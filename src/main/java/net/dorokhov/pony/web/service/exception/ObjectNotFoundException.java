package net.dorokhov.pony.web.service.exception;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ObjectNotFoundException extends Exception {
    
    private final Class objectType;
    private final Long objectId;

    public ObjectNotFoundException(Class objectType) {
        super(String.format("Object of type '%s' not found.", objectType.toString()));
        this.objectType = checkNotNull(objectType);
        this.objectId = null;
    }

    public ObjectNotFoundException(Class objectType, Long objectId) {
        super(String.format("Object '%d' of type '%s' not found.", objectId, objectType.toString()));
        this.objectType = checkNotNull(objectType);
        this.objectId = checkNotNull(objectId);
    }

    public Class getObjectType() {
        return objectType;
    }

    @Nullable
    public Long getObjectId() {
        return objectId;
    }
}
