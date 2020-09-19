package com.zacharytalis.alttextbot.commands;

import com.google.common.base.Stopwatch;
import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.logging.Logger;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.Toolbox;
import com.zacharytalis.alttextbot.utils.functions.Runnables;

import java.util.concurrent.CompletableFuture;

public abstract class BaseCommandBody implements CommandBody {
    private final AltTextBot bot;
    private final Logger logger;

    public BaseCommandBody(final AltTextBot bot) {
        this.bot = bot;

        // { BotName :: CommandClass (CommandName)}
        final var logPrefix = "{" + bot.internalName() + " :: " + getClass().getSimpleName() + " (" + getName() + ")}";
        this.logger = Toolbox.getLogger(this.getClass(), logPrefix);
    }

    @Override
    public final CompletableFuture<Void> executeAsync(CommandMessage msg) {
        return
            Toolbox
                .nullFuture()
                .thenRunAsync(Runnables.fromConsumer(this::preCommand, msg))            // Pre-execution
                .thenRun(() -> recordDuration(Toolbox.timed(this::call, msg)))          // Timed Execution
                .thenRun(Runnables.fromConsumer(this::postCommand, msg))                // Post-execution
                .exceptionally(Toolbox.acceptAndRethrow(this::handleError));            // Error handling
        // Errors will be rethrown for another exception handler
    }

    protected AltTextBot bot() {
        return bot;
    }

    protected Logger logger() {
        return logger;
    }

    protected abstract void call(CommandMessage msg);

    protected void preCommand(final CommandMessage msg) {
        logger().debug("Command execution starting");
    }

    protected void recordDuration(final Stopwatch result) {
        logger().debug("Execution completed in {}", result);
    }

    protected void postCommand(final CommandMessage msg) {
        logger().debug("Command execution complete");
    }

    protected void handleError(final Throwable t) {
        logger().error("Error occurred", t);
    }
}
