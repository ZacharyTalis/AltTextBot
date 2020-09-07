package com.zacharytalis.alttextbot.utils;

public interface ReadOnly<T extends ReadOnly<T>> {
    T readOnly();
}
