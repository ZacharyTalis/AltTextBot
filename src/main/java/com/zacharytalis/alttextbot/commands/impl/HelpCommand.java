package com.zacharytalis.alttextbot.commands.impl;

import com.zacharytalis.alttextbot.DiscordAPI;
import com.zacharytalis.alttextbot.commands.Command;
import com.zacharytalis.alttextbot.commands.CommandRegistry;
import com.zacharytalis.alttextbot.entities.message.Message;
import com.zacharytalis.alttextbot.utils.ReadOnly;

public class HelpCommand extends Command {
    @Override
    public String getName() {
        return "!atbhelp";
    }

    @Override
    public String getInfo() {
        return "Get all commands from AltTextBot in a direct message.";
    }

    @Override
    protected void call(ReadOnly<CommandRegistry> registry, DiscordAPI api, Message msg) {
        StringBuilder helpText =
            registry
            .readOnly()
            .values()
            .stream()
            .reduce(
                new StringBuilder(),
                (sb, cmd) -> sb.append(cmd.getName()).append(" ~ ").append(cmd.getInfo()).append('\n'),
                StringBuilder::append
            );

        msg.getAuthor().sendMessage(helpText.toString());
    }
}
