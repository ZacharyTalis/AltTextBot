package com.zacharytalis.alttextbot.bangCommands;

import com.zacharytalis.alttextbot.utils.CommandMessage;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface CommandBody extends Consumer<CommandMessage>, Function<CommandMessage, CompletableFuture<Void>> {
    CommandInfo getInfo();

    default String getCommandPrefix() {
        return getInfo().bangName();
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
