package net.dorokhov.pony2.web.service.exception;

import jakarta.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ObjectNotFoundException extends Exception {
    
    private final Class<?> objectType;
    private final String objectId;

    public ObjectNotFoundException(Class<?> objectType) {
        super(String.format("Object of type '%s' not found.", objectType.toString()));
        this.objectType = checkNotNull(objectType);
        this.objectId = null;
    }

    public ObjectNotFoundException(Class<?> objectType, String objectId) {
        super(String.format("Object '%s' of type '%s' not found.", objectId, objectType.toString()));
        this.objectType = checkNotNull(objectType);
        this.objectId = checkNotNull(objectId);
    }

    public Class<?> getObjectType() {
        return objectType;
    }

    @Nullable
    public String getObjectId() {
        return objectId;
    }
}
