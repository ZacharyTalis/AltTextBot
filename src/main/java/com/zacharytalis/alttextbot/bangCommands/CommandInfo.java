package com.zacharytalis.alttextbot.bangCommands;

import com.zacharytalis.alttextbot.bots.AltTextBot;

import java.util.function.Function;

public record CommandInfo(String name, String helpInfo, Function<AltTextBot, CommandBody> factory) {
    public String bangName() {
        return "!" + this.name;
    }

    public CommandBody instantiate(AltTextBot bot) {
        return factory.apply(bot);
    }
}
