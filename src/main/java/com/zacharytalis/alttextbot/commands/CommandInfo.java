package com.zacharytalis.alttextbot.commands;

import com.zacharytalis.alttextbot.bots.AltTextBot;

import java.util.function.Function;

public record CommandInfo(String name, String helpInfo, Function<AltTextBot, CommandBody> factory) {
    public CommandBody instantiate(AltTextBot bot) {
        return factory.apply(bot);
    }
}
