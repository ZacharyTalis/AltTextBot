package com.zacharytalis.alttextbot.utils;

public interface ReadOnly<T> {
    class AttemptedWriteException extends RuntimeException {
        public AttemptedWriteException(String context) {
            super("Attempted to write to read only object: " + context);
        }
    }

    T readOnly();
}
