package net.dorokhov.pony.library.service.exception;

public class ObjectNotFoundException extends Exception {

    private final long id;
    private final Class objectClass;

    public ObjectNotFoundException(Long id, Class objectClass) {
        super(String.format("Object '%d' not found.", id));
        this.id = id;
        this.objectClass = objectClass;
    }

    public long getId() {
        return id;
    }

    public Class getObjectClass() {
        return objectClass;
    }
}
