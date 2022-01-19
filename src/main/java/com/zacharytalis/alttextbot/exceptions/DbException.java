package com.zacharytalis.alttextbot.exceptions;

public class DbException extends RuntimeException {
    public DbException(String msg) {
        super(msg);
    }

    public DbException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public DbException(String format, Object... args) {
        this(String.format(format, args));
    }

    public DbException(Throwable cause, String format, Object... args) {
        this(String.format(format, args), cause);
    }
}
