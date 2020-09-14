package com.zacharytalis.alttextbot.commands;

import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.utils.AnticipatedValue;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.Messages;
import com.zacharytalis.alttextbot.utils.ReadOnly;
import org.javacord.api.entity.message.Message;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ICommandRegistry<T extends ICommandRegistry<T>> extends Map<String, CommandInfo>, ReadOnly<T> {
    default T registerFunctionally(String name, String help, Function<AltTextBot, Consumer<CommandMessage>> body) {
        final AnticipatedValue<CommandInfo> infoProvider = new AnticipatedValue<>();

        final var info = new CommandInfo(
            name,
            help,
            bot -> new ICommandBody() {
                @Override
                public CommandInfo getInfo() {
                    return infoProvider.demand();
                }

                @Override
                public CompletableFuture<Void> executeAsync(CommandMessage msg) {
                    final var cmdBody = body.apply(bot);
                    return CompletableFuture.runAsync(() -> cmdBody.accept(msg));
                }
            }
        );

        infoProvider.provide(info);

        return register(info);
    }

    default T register(String name, String help, Function<AltTextBot, ICommandBody> body) {
        return register(new CommandInfo(
            name,
            help,
            body
        ));
    }

    @SuppressWarnings("unchecked")
    default T register(CommandInfo info) {
        put(info.name(), info);
        return (T) this;
    }

    default T register(CommandInfo info1, CommandInfo info2, CommandInfo... infos) {
        final var self = register(info1); register(info2);
        for (CommandInfo info : infos) {
            register(info);
        }

        return self;
    }

    default T alias(String newName, String oldName) {
        final var oldInfo = get(oldName);
        if (oldInfo == null)
            throw new NullPointerException(oldName + " does not exist in the registry to alias");

        final var newInfo = new CommandInfo(
            newName,
            oldInfo.helpInfo(),
            oldInfo.factory()
        );

        return register(newInfo);
    }

    default ICommandBody prepareCommand(CommandMessage msg, AltTextBot bot) {
        return get(msg).instantiate(bot);
    }

    default ICommandBody prepareCommand(Message msg, AltTextBot bot) {
         return get(msg).instantiate(bot);
    }

    default ICommandBody prepareCommand(String name, AltTextBot bot) {
        return get(name).instantiate(bot);
    }

    default CommandInfo get(CommandMessage msg) {
        return get(msg.getCommandPrefix());
    }

    default CommandInfo get(Message msg) {
        return get(Messages.asCommandMessage(msg));
    }

    default boolean containsKey(CommandMessage key) {
        return containsKey(key.getCommandPrefix());
    }

    default boolean containsKey(Message key) {
        return containsKey(Messages.asCommandMessage(key));
    }
}
