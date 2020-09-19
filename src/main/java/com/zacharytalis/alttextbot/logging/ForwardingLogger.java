package com.zacharytalis.alttextbot.logging;

import com.google.common.collect.ForwardingObject;
import org.slf4j.Marker;

public abstract class ForwardingLogger extends ForwardingObject implements Logger {
    protected final org.slf4j.Logger logger;

    ForwardingLogger(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    @Override
    protected org.slf4j.Logger delegate() {
        return logger;
    }

    @Override
    public String getName() {
        return delegate().getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return delegate().isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        if (shouldLog(msg))
            delegate().trace(finalizeMessage(msg));
    }

    @Override
    public void trace(String format, Object arg) {
        trace(formatLogMessage(format, arg));
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        trace(formatLogMessage(format, arg1, arg2));
    }

    @Override
    public void trace(String format, Object... arguments) {
        trace(formatLogMessage(format, arguments));
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (shouldLog(msg))
            delegate().trace(finalizeMessage(msg), t);
    }

    @Override
    public void trace(Throwable t, String format, Object... arguments) {
        trace(formatLogMessage(format, arguments), t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegate().isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        if (shouldLog(msg))
            delegate().trace(marker, finalizeMessage(msg));
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        trace(marker, formatLogMessage(format, arg));
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        trace(marker, formatLogMessage(format, arg1, arg2));
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        trace(marker, formatLogMessage(format, argArray));
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        if (shouldLog(msg))
            delegate().trace(marker, finalizeMessage(msg), t);
    }

    @Override
    public void trace(Marker marker, Throwable t, String format, Object... arguments) {
        trace(marker, formatLogMessage(format, arguments), t);
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate().isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        if (shouldLog(msg))
            delegate().debug(finalizeMessage(msg));
    }

    @Override
    public void debug(String format, Object arg) {
        debug(formatLogMessage(format, arg));
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        debug(formatLogMessage(format, arg1, arg2));
    }

    @Override
    public void debug(String format, Object... arguments) {
        debug(formatLogMessage(format, arguments));
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (shouldLog(msg))
            delegate().debug(finalizeMessage(msg), t);
    }

    @Override
    public void debug(Throwable t, String format, Object... arguments) {
        debug(formatLogMessage(format, arguments), t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return delegate().isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        if (shouldLog(msg))
            delegate().debug(marker, finalizeMessage(msg));
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        debug(marker, formatLogMessage(format, arg));
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        debug(marker, formatLogMessage(format, arg1, arg2));
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        debug(marker, formatLogMessage(format, arguments));
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        if (shouldLog(msg))
            delegate().debug(marker, finalizeMessage(msg), t);
    }

    @Override
    public void debug(Marker marker, Throwable t, String format, Object... arguments) {
        debug(marker, formatLogMessage(format, arguments), t);
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate().isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        if (shouldLog(msg))
            delegate().info(finalizeMessage(msg));
    }

    @Override
    public void info(String format, Object arg) {
        info(formatLogMessage(format, arg));
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        info(formatLogMessage(format, arg1, arg2));
    }

    @Override
    public void info(String format, Object... arguments) {
        info(formatLogMessage(format, arguments));
    }

    @Override
    public void info(String msg, Throwable t) {
        if (shouldLog(msg))
            delegate().info(finalizeMessage(msg), t);
    }

    @Override
    public void info(Throwable t, String format, Object... arguments) {
        info(formatLogMessage(format, arguments), t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return delegate().isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        if (shouldLog(msg))
            delegate().info(marker, finalizeMessage(msg));
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        info(marker, formatLogMessage(format, arg));
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        info(marker, formatLogMessage(format, arg1, arg2));
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        info(marker, formatLogMessage(format, arguments));
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        if (shouldLog(msg))
            delegate().info(marker, finalizeMessage(msg), t);
    }

    @Override
    public void info(Marker marker, Throwable t, String format, Object... arguments) {
        info(marker, formatLogMessage(format, arguments), t);
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate().isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        if (shouldLog(msg))
            delegate().warn(finalizeMessage(msg));
    }

    @Override
    public void warn(String format, Object arg) {
        warn(formatLogMessage(format, arg));
    }

    @Override
    public void warn(String format, Object... arguments) {
        warn(formatLogMessage(format, arguments));
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        warn(formatLogMessage(format, arg1, arg2));
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (shouldLog(msg))
            delegate().warn(finalizeMessage(msg), t);
    }

    @Override
    public void warn(Throwable t, String format, Object... arguments) {
        warn(formatLogMessage(format, arguments), t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return delegate().isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        if (shouldLog(msg))
            delegate().warn(marker, finalizeMessage(msg));
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        warn(marker, formatLogMessage(format, arg));
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        warn(marker, formatLogMessage(format, arg1, arg2));
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        warn(marker, formatLogMessage(format, arguments));
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        if (shouldLog(msg))
            delegate().warn(marker, finalizeMessage(msg), t);
    }

    @Override
    public void warn(Marker marker, Throwable t, String format, Object... arguments) {
        warn(marker, formatLogMessage(format, arguments), t);
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate().isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        if (shouldLog(msg))
            delegate().error(finalizeMessage(msg));
    }

    @Override
    public void error(String format, Object arg) {
        error(formatLogMessage(format, arg));
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        error(formatLogMessage(format, arg1, arg2));
    }

    @Override
    public void error(String format, Object... arguments) {
        error(formatLogMessage(format, arguments));
    }

    @Override
    public void error(String msg, Throwable t) {
        if (shouldLog(msg))
            delegate().error(finalizeMessage(msg), t);
    }

    @Override
    public void error(Throwable t, String format, Object... arguments) {
        error(formatLogMessage(format, arguments), t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return delegate().isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        if (shouldLog(msg))
            delegate().error(marker, finalizeMessage(msg));
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        error(marker, formatLogMessage(format, arg));
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        error(marker, formatLogMessage(format, arg1, arg2));
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        error(marker, formatLogMessage(format, arguments));
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        if (shouldLog(msg))
            delegate().error(marker, finalizeMessage(msg), t);
    }

    @Override
    public void error(Marker marker, Throwable t, String format, Object... arguments) {
        error(marker, formatLogMessage(format, arguments), t);
    }

    protected String finalizeMessage(String message) {
        return message;
    }

    protected boolean shouldLog(String message) {
        return true;
    }

    protected String formatLogMessage(String format, Object... arguments) {
        if (delegate() instanceof ForwardingLogger fwd) {
            return format(fwd, format, arguments);
        } else {
            return format(format, arguments);
        }
    }

    protected abstract String format(String format, Object... arguments);
    protected abstract String format(ForwardingLogger decorated, String format, Object... arguments);
}
