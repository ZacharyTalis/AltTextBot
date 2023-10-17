package com.zacharytalis.alttextbot.utils.functions;

import java.util.function.Consumer;
import java.util.function.Function;

public class Runnables {
    public static <T> Runnable fromConsumer(Consumer<T> consumer, T arg) {
        return () -> consumer.accept(arg);
    }

    public static <T, U> Runnable fromFunction(Function<T, U> fn, T val) {
        return () -> {
            fn.apply(val);
        };
    }
}
