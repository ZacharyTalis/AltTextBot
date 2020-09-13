package com.zacharytalis.alttextbot;

import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.commands.CommandRegistry;
import com.zacharytalis.alttextbot.commands.impl.AltCommand;
import com.zacharytalis.alttextbot.commands.impl.HelpCommand;
import com.zacharytalis.alttextbot.commands.impl.PingCommand;

import static com.zacharytalis.alttextbot.utils.Ref.TOKEN_VAR;

public class EntryPoint {
    public static void main(String[] args) {
        var token = System.getenv(TOKEN_VAR);
        var cmds = new CommandRegistry();

        if (token == null)
            throw new NullPointerException("No environment variable: BOT_TOKEN");

        cmds.register(
            HelpCommand.description(),
            PingCommand.description(),
            AltCommand.description()
        );

        final var bot = new AltTextBot(token, cmds);
        bot.start().join();
    }
}
