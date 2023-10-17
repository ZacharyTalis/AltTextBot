package com.zacharytalis.alttextbot.bots;

import com.zacharytalis.alttextbot.utils.config.Config;
import org.javacord.api.DiscordApi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface DiscordBot<T extends DiscordBot<T>> extends Runnable, DiscordBotInfo {
    CompletableFuture<T> start();

    default void run() {
        start();
    }

    CompletableFuture<DiscordApi> api() throws NotStartedException;

    CompletableFuture<Void> whenApiAvailable(Consumer<DiscordApi> action) throws NotStartedException;

    Config config();
}
