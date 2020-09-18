package com.zacharytalis.alttextbot.logging;

import com.zacharytalis.alttextbot.utils.Toolbox;

public class PrefixingLogger extends ForwardingLogger {
    private final String prefix;

    public PrefixingLogger(Logger wrappedLogger, String prefix) {
        super(wrappedLogger);
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    protected String finalizeMessage(String message) {
        return prefix + " " + message;
    }

    @Override
    protected String format(ForwardingLogger decorated, String format, Object... arguments) {
        return decorated.formatLogMessage(format, arguments);
    }

    @Override
    protected String format(String format, Object... arguments) {
        return Toolbox.loggerFormat(format, arguments);
    }
}
