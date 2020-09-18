package com.zacharytalis.alttextbot.exceptions;

public class InvalidEnvironmentError extends Exception {
    public InvalidEnvironmentError(String msg) {
        super(msg);
    }

    public InvalidEnvironmentError(String msg, Throwable cause) {
        super(msg, cause);
    }

    public InvalidEnvironmentError(String format, Object... args) {
        this(String.format(format, args));
    }

    public InvalidEnvironmentError(Throwable cause, String format, Object... args) {
        this(String.format(format, args), cause);
    }
}
