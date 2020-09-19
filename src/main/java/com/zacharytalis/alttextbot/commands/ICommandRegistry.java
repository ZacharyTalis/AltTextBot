package com.zacharytalis.alttextbot.commands;

import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.Messages;
import com.zacharytalis.alttextbot.utils.ReadOnly;
import org.javacord.api.entity.message.Message;

import java.util.Map;

public interface ICommandRegistry<T extends ICommandRegistry<T>> extends Map<String, CommandInfo>, ReadOnly<T> {
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

    default CommandBody prepareCommand(CommandMessage msg, AltTextBot bot) {
        return get(msg).instantiate(bot);
    }

    default CommandBody prepareCommand(Message msg, AltTextBot bot) {
         return get(msg).instantiate(bot);
    }

    default CommandBody prepareCommand(String name, AltTextBot bot) {
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
