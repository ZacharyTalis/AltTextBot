package com.zacharytalis.alttextbot.bots;

import com.zacharytalis.alttextbot.commands.CommandRegistry;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.ReadOnly;
import com.zacharytalis.alttextbot.utils.functions.Functions;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AltTextBot implements DiscordBot<AltTextBot> {
    private static final Logger logger = LoggerFactory.getLogger(AltTextBot.class);

    private final CommandRegistry commands;
    private final DiscordApiBuilder apiBuilder;
    private CompletableFuture<DiscordApi> discordApi;

    public AltTextBot(final String token, final CommandRegistry registry) {
        this.commands = registry;
        this.apiBuilder =
            new DiscordApiBuilder()
                .setToken(token);
    }

    @Override
    public CompletableFuture<AltTextBot> start() {
        this.discordApi = this.apiBuilder.login();

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

    private void assertStarted() throws NotStartedException {
        if (this.discordApi == null)
            throw new NotStartedException();
    }

    private void addBotListeners(DiscordApi api) {
        // Listen for *any* commands
        api.addMessageCreateListener(event -> {
            final var msg = new CommandMessage(event.getMessage());

            if(commands.containsKey(msg))
                commands.prepareCommand(msg.getCommandPrefix(), AltTextBot.this).execute(msg);
        });
    }
}
