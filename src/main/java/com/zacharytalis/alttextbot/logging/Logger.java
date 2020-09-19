package com.zacharytalis.alttextbot.logging;

import com.zacharytalis.alttextbot.utils.Ref;
import org.slf4j.Marker;

import java.util.function.Supplier;

public interface Logger extends org.slf4j.Logger {
    void trace(Throwable t, String format, Object... args);
    void trace(Marker marker, Throwable t, String format, Object... args);

    void debug(Throwable t, String format, Object... args);
    void debug(Marker marker, Throwable t, String format, Object... args);

    void info(Throwable t, String format, Object... args);
    void info(Marker marker, Throwable t, String format, Object... args);

    void warn(Throwable t, String format, Object... args);
    void warn(Marker marker, Throwable t, String format, Object... args);

    void error(Throwable t, String format, Object... args);
    void error(Marker marker, Throwable t, String format, Object... args);

    default Logger testingOnly(Supplier<Ref.EnvType> currentEnv) {
        if (currentEnv.get().isTesting())
            return this;
        else
            return NullLogger.instance();
    }

    default Logger testingOnly() {
        return testingOnly(Ref::currentEnv);
    }
}
