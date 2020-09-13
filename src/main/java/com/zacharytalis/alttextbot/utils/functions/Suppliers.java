package com.zacharytalis.alttextbot.utils.functions;

import java.util.function.Supplier;

public class Suppliers {
    public static <T> Supplier<T> supplying(T value) {
        return () -> value;
    }
}
