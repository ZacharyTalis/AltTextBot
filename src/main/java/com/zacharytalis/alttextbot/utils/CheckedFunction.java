package com.zacharytalis.alttextbot.utils;

@FunctionalInterface
public interface CheckedFunction<T, R> {
    R apply(T in) throws Throwable;
}
