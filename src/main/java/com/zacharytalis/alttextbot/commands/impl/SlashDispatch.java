package com.zacharytalis.alttextbot.commands.impl;

import com.zacharytalis.alttextbot.commands.ICommandDispatch;
import com.zacharytalis.alttextbot.messages.UserCommandMessage;
import com.zacharytalis.alttextbot.slashCommands.SlashCommandHandler;
import com.zacharytalis.alttextbot.utils.Toolbox;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SlashDispatch implements ICommandDispatch {
    private final Map<String, SlashCommandHandler> handlerMap = new HashMap<>();

    public SlashDispatch(Iterable<SlashCommandHandler> handlers) {
        for (var handler : handlers)
            this.handlerMap.put(handler.name(), handler);
    }

    @Override
    public CompletableFuture<Void> dispatch(UserCommandMessage userMessage) {
        final var msg = (UserCommandMessage.Slash) userMessage;
        final var handlerOption = findHandler(msg.interaction());

        return handlerOption.map(handler -> {
            return Toolbox.voidFuture(handler.receive(msg.bot(), msg.interaction()));
        }).orElse(Toolbox.nullFuture());
    }

    private Optional<SlashCommandHandler> findHandler(SlashCommandInteraction inter) {
        final var name = inter.getFullCommandName();
        return Optional.ofNullable(this.handlerMap.get(name));
    }

    @Override
    public boolean canDispatch(UserCommandMessage msg) {
        if (msg instanceof UserCommandMessage.Slash slashMsg) {
            return findHandler(slashMsg.interaction()).isPresent();
        }

        return false;
    }
}
