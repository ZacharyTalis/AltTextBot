package com.zacharytalis.alttextbot.commands;

import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseCommandBody implements ICommandBody {
    private final AltTextBot bot;
    private final Logger logger;

    public BaseCommandBody(final AltTextBot bot) {
        this.bot = bot;
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public final void execute(CommandMessage msg) {
        preCommand(msg);
        this.call(msg);
        postCommand(msg);
    }

    protected AltTextBot getBot() {
        return bot;
    }

    protected Logger getLogger() {
        return logger;
    }

    protected abstract void call(CommandMessage msg);

    protected void preCommand(final CommandMessage msg) {
        getLogger().debug("Command received: {}", getName());
    }

    protected void postCommand(final CommandMessage msg) {
        getLogger().debug("Command completed: {}", getName());
    }
}
