package com.zacharytalis.alttextbot.commands;

import com.zacharytalis.alttextbot.DiscordAPI;
import com.zacharytalis.alttextbot.entities.message.Message;
import com.zacharytalis.alttextbot.utils.ReadOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Command implements ICommand {
    private final Logger logger;

    protected Command() {
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public final void execute(ReadOnly<CommandRegistry> cmd, DiscordAPI api, Message msg) {
        getLogger().debug("Command received: {}", getName());
        this.call(cmd, api, msg);
        getLogger().debug("Command completed: {}", getName());
    }

    protected Logger getLogger() {
        return logger;
    }

    protected abstract void call(ReadOnly<CommandRegistry> cmd, DiscordAPI api, Message msg);
}
