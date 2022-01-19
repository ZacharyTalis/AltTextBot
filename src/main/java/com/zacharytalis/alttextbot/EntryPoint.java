package com.zacharytalis.alttextbot;

import com.zacharytalis.alttextbot.board.BoardUtils;
import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.commands.registry.CommandRegistry;
import com.zacharytalis.alttextbot.commands.impl.*;
import com.zacharytalis.alttextbot.db.ConnectionPool;
import com.zacharytalis.alttextbot.exceptions.InvalidEnvironmentException;
import com.zacharytalis.alttextbot.logging.Logger;
import com.zacharytalis.alttextbot.utils.Configs;
import com.zacharytalis.alttextbot.utils.Toolbox;
import com.zacharytalis.alttextbot.utils.config.ConfigurationException;

import java.io.IOException;

public class EntryPoint {
    public static final Logger logger = Toolbox.getLogger("StartUp");

    public static void main(String[] args) throws InvalidEnvironmentException, ConfigurationException, IOException {
        final var config = Configs.getConfigFromEnv();
        final var cmds = new CommandRegistry();

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

        // Convert board files to database on first run (should only do this once)
        BoardUtils.getAllBoardFiles().forEach(bs -> {
            final var result = bs.migrateToDatabase();
            logger.info("Migrated {} contributions from filesystem to database for server ID {}", result.size(), bs.serverID);
        });

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
