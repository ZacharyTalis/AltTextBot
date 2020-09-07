package com.zacharytalis.alttextbot.commands;

import com.zacharytalis.alttextbot.DiscordAPI;
import com.zacharytalis.alttextbot.entities.message.Message;
import com.zacharytalis.alttextbot.utils.ReadOnly;
import com.zacharytalis.alttextbot.utils.functions.TriConsumer;

public interface ICommand extends TriConsumer<ReadOnly<CommandRegistry>, DiscordAPI, Message> {
    String getName();
    String getInfo();

    void execute(ReadOnly<CommandRegistry> cmd, DiscordAPI api, Message msg);

    default void accept(ReadOnly<CommandRegistry> cmd, DiscordAPI api, Message msg) {
        execute(cmd, api, msg);
    }
}
