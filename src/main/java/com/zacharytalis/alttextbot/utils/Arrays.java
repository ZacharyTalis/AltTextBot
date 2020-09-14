package com.zacharytalis.alttextbot.utils;

import java.lang.reflect.Array;

public class Arrays {
    public static <T> void prepend(T object, T[] into, T[] output) {
        assert into != output;
        assert output.length > into.length;

        System.arraycopy(into, 0, output, 1, into.length);
        output[0] = object;
    }

    public static <T> T[] prepend(Class<? extends T> type, T object, T[] into) {
        final var output = create(type, into.length + 1);
        prepend(object, into, output);
        return output;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] create(Class<? extends T> type, int length) {
        return (T[]) Array.newInstance(type, length);
    }
}
