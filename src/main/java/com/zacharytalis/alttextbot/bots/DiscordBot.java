package com.zacharytalis.alttextbot.bots;

import com.zacharytalis.alttextbot.commands.registry.CommandRegistry;
import com.zacharytalis.alttextbot.commands.registry.ICommandRegistry;
import com.zacharytalis.alttextbot.utils.config.Config;
import org.javacord.api.DiscordApi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface DiscordBot<T extends DiscordBot<T>> extends Runnable {
    CompletableFuture<T> start();
    default void run() { start(); }

    String internalName();

    CompletableFuture<DiscordApi> api() throws NotStartedException;
    ICommandRegistry<CommandRegistry> commands();

    CompletableFuture<Void> whenApiAvailable(Consumer<DiscordApi> action) throws NotStartedException;
    Config config();
}
