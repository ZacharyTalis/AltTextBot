package com.zacharytalis.alttextbot.utils.config;

public class ConfigurationException extends Exception {
    public ConfigurationException(String msg) {
        super(msg);
    }

    public ConfigurationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public ConfigurationException(String format, Object... args) {
        this(String.format(format, args));
    }

    public ConfigurationException(Throwable cause, String format, Object... args) {
        this(String.format(format, args), cause);
    }
}
