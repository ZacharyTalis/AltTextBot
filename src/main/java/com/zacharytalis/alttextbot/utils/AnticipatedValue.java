package com.zacharytalis.alttextbot.utils;

import javax.annotation.Nonnull;
import java.util.Objects;

public class AnticipatedValue<T> {
    public static class NotFound extends RuntimeException {}

    private T value;

    public void provide(@Nonnull T value) {
        assert Objects.isNull(this.value);
        Objects.requireNonNull(value);

        this.value = value;
    }

    public T demand() {
        if (this.value == null)
            throw new NotFound();

        return this.value;
    }
}
