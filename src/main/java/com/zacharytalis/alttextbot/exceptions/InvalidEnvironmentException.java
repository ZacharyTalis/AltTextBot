package com.zacharytalis.alttextbot.exceptions;

public class InvalidEnvironmentException extends Exception {
    public InvalidEnvironmentException(String msg) {
        super(msg);
    }

    public InvalidEnvironmentException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public InvalidEnvironmentException(String format, Object... args) {
        this(String.format(format, args));
    }

    public InvalidEnvironmentException(Throwable cause, String format, Object... args) {
        this(String.format(format, args), cause);
    }
}
