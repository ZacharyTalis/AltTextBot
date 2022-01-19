package com.zacharytalis.alttextbot.utils;

@FunctionalInterface
public interface CheckedSupplier<T> {
    T get() throws Throwable;
}
