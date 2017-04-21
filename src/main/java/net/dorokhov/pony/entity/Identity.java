package net.dorokhov.pony.entity;

import java.io.Serializable;

public interface Identity<T extends Serializable> {
    T getId();
}
