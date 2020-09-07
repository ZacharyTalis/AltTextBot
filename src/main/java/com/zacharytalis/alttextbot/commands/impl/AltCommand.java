package com.zacharytalis.alttextbot.commands.impl;

import com.google.common.util.concurrent.FutureCallback;
import com.zacharytalis.alttextbot.DiscordAPI;
import com.zacharytalis.alttextbot.commands.Command;
import com.zacharytalis.alttextbot.commands.CommandRegistry;
import com.zacharytalis.alttextbot.entities.message.Message;
import com.zacharytalis.alttextbot.utils.ReadOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AltCommand extends Command {
    private static final String ERROR_ALT = "Error with alt-text functionality. Do I have the right permissions?";
    private static final Logger logger = LoggerFactory.getLogger(AltCommand.class);

    @Override
    public String getName() {
        return "!alt";
    }

    @Override
    public String getInfo() {
        return "Replace the user message with alt-text. Post your alt-text as a separate message with the " +
                "format `!alt [alt-text]` (no brackets).";
    }

    @Override
    protected void call(ReadOnly<CommandRegistry> registry, DiscordAPI api, Message recv) {
        recv.reply(getAltContent(recv), new FutureCallback<Message>() {
            @Override
            public void onSuccess(Message message) {
                recv.delete();
            }

            @Override
            public void onFailure(Throwable throwable) {
                recv.reply(ERROR_ALT);
                logger.error("Failed to reply with alt text.", throwable);
            }
        });
    }

    private String getAltContent(Message msg) {
        return msg.getContent().substring(getName().length()).trim();
    }
}
