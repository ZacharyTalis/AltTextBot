package com.zacharytalis.alttextbot;

import com.zacharytalis.alttextbot.bangCommands.impl.*;
import com.zacharytalis.alttextbot.bangCommands.registry.CommandRegistry;
import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.commands.dispatch.impl.BangDispatch;
import com.zacharytalis.alttextbot.commands.dispatch.impl.MultiDispatch;
import com.zacharytalis.alttextbot.commands.dispatch.impl.SlashDispatch;
import com.zacharytalis.alttextbot.db.ConnectionPool;
import com.zacharytalis.alttextbot.exceptions.InvalidEnvironmentException;
import com.zacharytalis.alttextbot.logging.Logger;
import com.zacharytalis.alttextbot.slashCommands.SlashCommandHandlerCollection;
import com.zacharytalis.alttextbot.slashCommands.impl.AboutCommandHandler;
import com.zacharytalis.alttextbot.slashCommands.impl.AltCommandHandler;
import com.zacharytalis.alttextbot.slashCommands.impl.BoardCommandHandler;
import com.zacharytalis.alttextbot.slashCommands.impl.PingCommandHandler;
import com.zacharytalis.alttextbot.utils.Configs;
import com.zacharytalis.alttextbot.utils.Inflections;
import com.zacharytalis.alttextbot.utils.Toolbox;
import com.zacharytalis.alttextbot.utils.config.ConfigurationException;
import org.javacord.api.interaction.ApplicationCommand;

import java.io.IOException;

public class EntryPoint {
    public static final Logger logger = Toolbox.getLogger("StartUp");

    public static void main(String[] args) throws InvalidEnvironmentException, ConfigurationException, IOException {
        final var config = Configs.getConfigFromEnv();
        final var cmds = new CommandRegistry();
        final var slashCmds = new SlashCommandHandlerCollection();

        logger.info("Starting up with {} env", config.getEnv());

        final var migrationResult = ConnectionPool.migrate();
        if (!migrationResult.success) {
            logger.error("Migrations failed!");
            System.exit(1);
        } else if (migrationResult.migrationsExecuted > 0) {
            logger.info("Migrated {} versions up to {}", migrationResult.migrationsExecuted, migrationResult.targetSchemaVersion);
        } else {
            logger.info("No migrations to execute.");
        }

        cmds.register(
            HelpCommand.description(),
            PingCommand.description(),
            AboutCommand.description(),
            BoardCommand.description(),
            AltCommand.description()
        );

        slashCmds.add(new AboutCommandHandler());
        slashCmds.add(new AltCommandHandler());
        slashCmds.add(new BoardCommandHandler());
        slashCmds.add(new PingCommandHandler());

        final var dispatch = new MultiDispatch(
            new BangDispatch(cmds),
            new SlashDispatch(slashCmds)
        );

        logger.info("Starting Alt Text Bot");
        final var bot = new AltTextBot(config, cmds, dispatch);

        bot.whenApiAvailable(api -> {
            var slashCmdsFuture =
                slashCmds
                    .setGlobalCommands(api)
                    .thenCompose(_void -> api.getGlobalSlashCommands());


            slashCmdsFuture.thenAccept(registeredCmds -> {
                final var commandNames = registeredCmds.stream().map(ApplicationCommand::getName).toList();
                logger.info("Slash commands registered: " + Inflections.join(commandNames));
            });
        });

        bot.start().join();
    }
}
