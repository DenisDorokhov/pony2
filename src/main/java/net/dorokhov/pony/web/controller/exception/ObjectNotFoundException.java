package net.dorokhov.pony.web.controller.exception;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ObjectNotFoundException extends Exception {
    
    private final Class objectType;
    private final Long objectId;

    public ObjectNotFoundException(Class objectType, Long objectId) {
        super(String.format("Object '%d' of type '%s' not found.", objectId, objectType.toString()));
        this.objectType = checkNotNull(objectType);
        this.objectId = checkNotNull(objectId);
    }

    public Class getObjectType() {
        return objectType;
    }

    public Long getObjectId() {
        return objectId;
    }
}
