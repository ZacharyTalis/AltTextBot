package com.zacharytalis.alttextbot;

import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.commands.registry.CommandRegistry;
import com.zacharytalis.alttextbot.commands.impl.*;
import com.zacharytalis.alttextbot.exceptions.InvalidEnvironmentException;
import com.zacharytalis.alttextbot.logging.Logger;
import com.zacharytalis.alttextbot.utils.Configs;
import com.zacharytalis.alttextbot.utils.Toolbox;
import com.zacharytalis.alttextbot.utils.config.ConfigurationException;

public class EntryPoint {
    public static final Logger logger = Toolbox.getLogger("StartUp");

    public static void main(String[] args) throws InvalidEnvironmentException, ConfigurationException {
        final var config = Configs.getConfigFromEnv();
        final var cmds = new CommandRegistry();

        logger.info("Starting up with {} env", config.getEnv());
        cmds.register(
            HelpCommand.description(),
            PingCommand.description(),
            AboutCommand.description(),
            BoardCommand.description(),
            AltCommand.description()
        );

        logger.info("Starting Alt Text Bot");
        final var bot = new AltTextBot(config, cmds);
        bot.start().join();
    }
}
