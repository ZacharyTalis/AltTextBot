package com.zacharytalis.alttextbot.utils;

import java.util.Iterator;

public class Inflections {
    public static <T> String join(Iterator<T> iterator) {
        if (!iterator.hasNext())
            return "";

        final var output = new StringBuilder(iterator.next().toString());

        while (iterator.hasNext()) {
            T current = iterator.next();

            if (iterator.hasNext()) {
                output.append(", ").append(current.toString());
            } else {
                output.append(" and ").append(current.toString());
            }
        }

        return output.toString();
    }

    public static <T> String join(Iterable<T> iterable) {
        return join(iterable.iterator());
    }
}
