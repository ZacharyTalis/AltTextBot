package com.zacharytalis.alttextbot.bots;

import com.zacharytalis.alttextbot.commands.CommandRegistry;
import com.zacharytalis.alttextbot.utils.ReadOnly;
import com.zacharytalis.alttextbot.utils.config.IConfig;
import org.javacord.api.DiscordApi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface DiscordBot<T extends DiscordBot<T>> extends Runnable {
    CompletableFuture<T> start();
    default void run() { start(); }

    String getInternalName();

    CompletableFuture<DiscordApi> getApi() throws NotStartedException;
    ReadOnly<CommandRegistry> getCommands();

    CompletableFuture<Void> whenApiAvailable(Consumer<DiscordApi> action) throws NotStartedException;
    IConfig getConfig();
}
