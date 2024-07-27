package net.dorokhov.pony3.web.dto;

import jakarta.annotation.Nonnull;

public class OptionalResponseDto<T> {

    private boolean present;
    private T value;

    public boolean isPresent() {
        return present;
    }

    public OptionalResponseDto<T> setPresent(boolean present) {
        this.present = present;
        return this;
    }

    public T getValue() {
        return value;
    }

    public OptionalResponseDto<T> setValue(T value) {
        this.value = value;
        return this;
    }

    public static <T> OptionalResponseDto<T> of(@Nonnull T value) {
        return new OptionalResponseDto<T>()
                .setPresent(true)
                .setValue(value);
    }

    public static <T> OptionalResponseDto<T> empty() {
        return new OptionalResponseDto<T>()
                .setPresent(false);
    }
}
