package com.zacharytalis.alttextbot.bots;

import com.zacharytalis.alttextbot.commands.registry.CommandRegistry;
import com.zacharytalis.alttextbot.commands.registry.ICommandRegistry;
import com.zacharytalis.alttextbot.logging.Logger;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.Futures;
import com.zacharytalis.alttextbot.utils.Ref;
import com.zacharytalis.alttextbot.utils.Toolbox;
import com.zacharytalis.alttextbot.utils.config.Config;
import com.zacharytalis.alttextbot.utils.config.ConfigurationException;
import com.zacharytalis.alttextbot.utils.functions.Functions;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AltTextBot implements DiscordBot<AltTextBot> {
    public static final Intent[] INTENTS = new Intent[]{
        Intent.GUILDS,
        Intent.GUILD_MESSAGES,
        Intent.GUILD_MEMBERS,
    };

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
                .setToken(config.getToken())
                .setIntents(AltTextBot.INTENTS);
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
    public ICommandRegistry<CommandRegistry> commands() {
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
            final var author = msg.getAuthorInfo();

            logger.testingOnly().info("Message received: {}", msg);

            if(!author.isYourself() && commands.containsPrefix(msg))
                commands.get(msg).orElseThrow().instantiate(AltTextBot.this).executeAsync(msg);
            else {
                Toolbox
                    .perEnv()
                    .inTesting(() -> logger.info("Ignoring message because it is either self or invalid. {}, known_command: {}", msg, commands.containsPrefix(msg)))
                    .inProduction(() -> {
                        if (msg.isCommandLike())
                            logger.warn("Ignoring command-like message because it is either self or invalid. prefix: {}, is_self: {}", msg.getCommandPrefix(), author.isYourself());
                    })
                ;
            }
        });

        api.addServerJoinListener(event -> {
            final var switcher = Toolbox.perEnv();
            final var serverId = event.getServer().getIdAsString();
            final var serverName = event.getServer().getName();

            switcher.inTesting(() -> logger.info("{} joined server: {} [{}]", internalName(), serverName, serverId));
            switcher.inProduction(() -> logger.info("{} joined server: {}", internalName(), serverId));
        });

        api.addServerLeaveListener(event -> {
            final var switcher = Toolbox.perEnv();
            final var serverId = event.getServer().getIdAsString();
            final var serverName = event.getServer().getName();

            switcher.inTesting(() -> logger.info("{} left server: {} [{}]", internalName(), serverName, serverId));
            switcher.inProduction(() -> logger.info("{} left server: {}", internalName(), serverId));
        });
    }
}
