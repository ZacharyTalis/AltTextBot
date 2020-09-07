package com.zacharytalis.alttextbot.commands.impl;

import com.zacharytalis.alttextbot.DiscordAPI;
import com.zacharytalis.alttextbot.commands.Command;
import com.zacharytalis.alttextbot.commands.CommandRegistry;
import com.zacharytalis.alttextbot.entities.message.Message;
import com.zacharytalis.alttextbot.utils.ReadOnly;

public class PingCommand extends Command {
    @Override
    public String getName() {
        return "!atbping";
    }

    @Override
    public String getInfo() {
        return "Check to see if AltTextBot is alive.";
    }

    @Override
    protected void call(ReadOnly<CommandRegistry> registry, DiscordAPI api, Message msg) {
        msg.reply("Yes yes, I'm here.");
    }
}
