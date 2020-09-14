package com.zacharytalis.alttextbot.commands;

import com.zacharytalis.alttextbot.utils.CommandMessage;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ICommandBody extends Consumer<CommandMessage>, Function<CommandMessage, CompletableFuture<Void>> {
    CommandInfo getInfo();

    default String getName() {
        return getInfo().name();
    }

    default String getHelp() {
        return getInfo().helpInfo();
    }

    CompletableFuture<Void> executeAsync(CommandMessage msg);

    default void execute(CommandMessage msg) {
        executeAsync(msg).join();
    }

    default void accept(CommandMessage msg) {
        execute(msg);
    }

    default CompletableFuture<Void> apply(CommandMessage msg) {
        return executeAsync(msg);
    }
}
