package com.zacharytalis.alttextbot.bots;

import com.zacharytalis.alttextbot.commands.CommandRegistry;
import com.zacharytalis.alttextbot.logging.Logger;
import com.zacharytalis.alttextbot.utils.*;
import com.zacharytalis.alttextbot.utils.config.Config;
import com.zacharytalis.alttextbot.utils.config.ConfigurationException;
import com.zacharytalis.alttextbot.utils.functions.Functions;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AltTextBot implements DiscordBot<AltTextBot> {
    private static final Logger logger = Toolbox.inferLogger();

    private final CommandRegistry commands;
    private final DiscordApiBuilder apiBuilder;
    private final Config config;
    private CompletableFuture<DiscordApi> discordApi;

    public AltTextBot(final Config config, final CommandRegistry registry) throws ConfigurationException {
        this.commands = registry;
        this.config = config;

        logger.info("Bot Initialized");

        this.apiBuilder =
            new DiscordApiBuilder()
                .setToken(config.getToken());
    }

    @Override
    public CompletableFuture<AltTextBot> start() {
        this.discordApi = Futures.runThenGet(() -> logger.info("Logging In..."), this.apiBuilder::login);

        this.discordApi.thenAccept(api -> {
            final var link = api.createBotInvite(Ref.REQUIRED_PERMS);
            logger.info("{} Invite Link: {}", internalName(), link);
        });

        var future = new CompletableFuture<AltTextBot>();

        try {
            whenApiAvailable(this::addBotListeners)
                .thenAcceptAsync(unused -> {
                    future.complete(AltTextBot.this);
                })
                .exceptionally(
                    Functions.nullify(future::completeExceptionally)
                );
        } catch(NotStartedException ex) {
            logger.error("Somehow bot is not started in run?", ex);
        }

        return future;
    }

    @Override
    public CompletableFuture<Void> whenApiAvailable(Consumer<DiscordApi> action) {
        return this.api().thenAcceptAsync(action);
    }

    @Override
    public CompletableFuture<DiscordApi> api() {
        assertStarted();

        return this.discordApi;
    }

    @Override
    public String internalName() {
        return "AltTextBot";
    }

    @Override
    public ReadOnly<CommandRegistry> commands() {
        return this.commands.readOnly();
    }

    @Override
    public Config config() {
        return config;
    }

    private void assertStarted() throws NotStartedException {
        if (this.discordApi == null)
            throw new NotStartedException();
    }

    private void addBotListeners(DiscordApi api) {
        // Listen for *any* commands
        api.addMessageCreateListener(event -> {
            final var msg = new CommandMessage(event.getMessage());

            logger.testingOnly().info("Message received: {}", msg);

            if(!msg.getAuthorInfo().isYourself() && commands.containsKey(msg))
                commands.prepareCommand(msg.getCommandPrefix(), AltTextBot.this).executeAsync(msg);
            else
                logger.testingOnly().info("Ignoring message because it is either self or invalid. {}, known_command: {}", msg, commands.containsKey(msg));
        });
    }
}
