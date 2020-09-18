package com.zacharytalis.alttextbot.bots;

import com.zacharytalis.alttextbot.commands.CommandRegistry;
import com.zacharytalis.alttextbot.logging.Logger;
import com.zacharytalis.alttextbot.utils.*;
import com.zacharytalis.alttextbot.utils.config.ConfigurationException;
import com.zacharytalis.alttextbot.utils.config.IConfig;
import com.zacharytalis.alttextbot.utils.functions.Functions;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AltTextBot implements DiscordBot<AltTextBot> {
    private static final Logger logger = Toolbox.getLogger(AltTextBot.class);

    private final CommandRegistry commands;
    private final DiscordApiBuilder apiBuilder;
    private final IConfig config;
    private CompletableFuture<DiscordApi> discordApi;

    public AltTextBot(final IConfig config, final CommandRegistry registry) throws ConfigurationException {
        this.commands = registry;
        this.config = config;

        logger.debug("Bot Initialized");

        this.apiBuilder =
            new DiscordApiBuilder()
                .setToken(config.getToken());
    }

    @Override
    public CompletableFuture<AltTextBot> start() {
        this.discordApi = Futures.runThenGet(() -> logger.debug("Logging In..."), this.apiBuilder::login);

        this.discordApi.thenAccept(api -> {
            final var link = api.createBotInvite(Ref.REQUIRED_PERMS);
            logger.info("{} Invite Link: {}", getInternalName(), link);
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
    public CompletableFuture<Void> whenApiAvailable(Consumer<DiscordApi> action) throws NotStartedException {
        return this.getApi().thenAcceptAsync(action);
    }

    @Override
    public CompletableFuture<DiscordApi> getApi() throws NotStartedException {
        assertStarted();

        return this.discordApi;
    }

    @Override
    public String getInternalName() {
        return "AltTextBot";
    }

    @Override
    public ReadOnly<CommandRegistry> getCommands() {
        return this.commands.readOnly();
    }

    @Override
    public IConfig getConfig() {
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

            if(commands.containsKey(msg))
                commands.prepareCommand(msg.getCommandPrefix(), AltTextBot.this).executeAsync(msg);
        }).isGlobalListener();
    }
}
