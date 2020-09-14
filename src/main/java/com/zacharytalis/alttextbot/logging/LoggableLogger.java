package com.zacharytalis.alttextbot.logging;

import com.zacharytalis.alttextbot.utils.Toolbox;
import org.slf4j.Logger;

public class LoggableLogger extends ForwardingLogger {

    public LoggableLogger(Logger wrappedLogger) {
        super(wrappedLogger);
    }

    @Override
    protected String format(ForwardingLogger decorated, String format, Object... arguments) {
        Toolbox.toLoggableObjectArray(arguments);
        return decorated.formatLogMessage(format, arguments);
    }

    @Override
    protected String format(String format, Object... args) {
        return Toolbox.loggerFormat(format, args);
    }
}
