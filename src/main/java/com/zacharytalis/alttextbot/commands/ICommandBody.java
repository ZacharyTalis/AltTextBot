package com.zacharytalis.alttextbot.commands;

import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.utils.CommandMessage;

import java.util.function.Consumer;

public interface ICommandBody extends Consumer<CommandMessage> {
    CommandInfo getInfo();

    default String getName() {
        return getInfo().name();
    }

    default String getHelp() {
        return getInfo().helpInfo();
    }

    void execute(CommandMessage msg);

    default void accept(CommandMessage msg) {
        execute(msg);
    }
}
