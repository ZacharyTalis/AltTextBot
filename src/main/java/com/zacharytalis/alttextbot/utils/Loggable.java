package com.zacharytalis.alttextbot.utils;

public interface Loggable {
    String toLoggerString();

    default String toYesNo(boolean value) {
        return value ? "YES" : "NO";
    }
}
