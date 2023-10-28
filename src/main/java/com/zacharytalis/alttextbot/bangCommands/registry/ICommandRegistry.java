package com.zacharytalis.alttextbot.bangCommands.registry;

import com.zacharytalis.alttextbot.bangCommands.CommandInfo;
import com.zacharytalis.alttextbot.bangCommands.CommandMessage;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ICommandRegistry<T extends ICommandRegistry<T>> {
    T register(CommandInfo info);

    default T register(CommandInfo info1, CommandInfo info2, CommandInfo... infos) {
        final var self = register(info1);
        register(info2);
        for (CommandInfo info : infos) {
            register(info);
        }

        return self;
    }

    T alias(String newName, String oldName);

    boolean containsPrefix(String prefix);

    Optional<CommandInfo> get(String name);

    Collection<CommandInfo> values();
}
