package com.zacharytalis.alttextbot.utils.functions;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class Functions {
    public static <T, R> R doNothing(T unused) {
        return null;
    }

    public static <T> Function<? super T, ? extends T> identity() {
        return x -> x;
    }

    public static <T, U, R> Function<T, R> compose(Function<U, R> f, Function<T, U> g) {
        return t -> f.apply(g.apply(t));
    }

    public static <T, U, R> Function<T, R> pipe(Function<T, U> g, Function<U, R> f) {
        return compose(f, g);
    }

    public static <T, U> Function<T, U> nullify(Consumer<T> fn) {
        return t -> { fn.accept(t); return null; };
    }

    public static <T, U, R> Function<U, R> partial(BiFunction<T, U, R> fn, T arg) {
        return u -> fn.apply(arg, u);
    }

    public static <T, U> Function<U, Void> partial(BiConsumer<T, U> fn, T arg) {
        return u -> {
            fn.accept(arg, u);
            return null;
        };
    }

    public static <T, U> Runnable partial(Function<T, U> fn, T val) {
        return () -> { fn.apply(val); };
    }
}
