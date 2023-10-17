package com.zacharytalis.alttextbot.commands.impl;

import com.zacharytalis.alttextbot.bangCommands.CommandBody;
import com.zacharytalis.alttextbot.bangCommands.registry.ICommandRegistry;
import com.zacharytalis.alttextbot.commands.ICommandDispatch;
import com.zacharytalis.alttextbot.messages.UserCommandMessage;
import com.zacharytalis.alttextbot.utils.Toolbox;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class BangDispatch implements ICommandDispatch {

    private final ICommandRegistry<?> registry;

    public BangDispatch(ICommandRegistry<?> registry) {
        this.registry = registry;
    }

    @Override
    public CompletableFuture<Void> dispatch(UserCommandMessage userMessage) {
        final var msg = (UserCommandMessage.Bang) userMessage;
        final var command = findCommand(msg);

        return command.map(cmd -> cmd.executeAsync(msg.commandMessage())).orElse(Toolbox.nullFuture());
    }

    private Optional<CommandBody> findCommand(UserCommandMessage.Bang msg) {
        return this.registry.get(msg.commandMessage()).map(info -> info.instantiate(msg.bot()));
    }

    @Override
    public boolean canDispatch(UserCommandMessage msg) {
        if (msg instanceof UserCommandMessage.Bang bangMsg) {
            return this.findCommand(bangMsg).isPresent();
        }

        return false;
    }
}
