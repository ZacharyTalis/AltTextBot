package com.zacharytalis.alttextbot;

import com.google.common.util.concurrent.FutureCallback;
import com.zacharytalis.alttextbot.commands.CommandRegistry;
import com.zacharytalis.alttextbot.commands.ICommand;
import com.zacharytalis.alttextbot.commands.impl.AltCommand;
import com.zacharytalis.alttextbot.commands.impl.HelpCommand;
import com.zacharytalis.alttextbot.commands.impl.PingCommand;
import com.zacharytalis.alttextbot.listener.message.MessageCreateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Bot for posting image alt-text as a separate Discord message.
 */
public class AltTextBot {

    // Constants
    private static final String BOT_TOKEN = System.getenv("BOT_TOKEN");

    private static final Logger logger = LoggerFactory.getLogger(AltTextBot.class);

    private final CommandRegistry commands;
    private final DiscordAPI discordApi;

    private AltTextBot(String token, CommandRegistry commands) {
        this.commands = commands;
        this.discordApi = Javacord.getApi(token, true);
    }

    public void start() {
        this.discordApi.connect(new FutureCallback<DiscordAPI>() {
            @Override
            public void onSuccess(DiscordAPI _api) {
                _api.registerListener((MessageCreateListener) (api, message) -> {
                    if (commands.containsKey(message))
                        commands.get(message).execute(commands, api, message);
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                logger.error("An error occurred on API connect.", throwable);
            }
        });
    }

    /**
     * Get the bot up and running.
     * @param args the executable arguments.
     */
    public static void main(String[] args) {
        final CommandRegistry registry = new CommandRegistry();

        registry
            .register(
                new HelpCommand(),
                new PingCommand(),
                new AltCommand()
            );

        new AltTextBot(BOT_TOKEN, registry).start();
    }

}
