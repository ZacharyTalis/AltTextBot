package com.zacharytalis.alttextbot.commands.registry;

import com.zacharytalis.alttextbot.commands.CommandInfo;
import com.zacharytalis.alttextbot.utils.CommandMessage;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ICommandRegistry<T extends ICommandRegistry<T>> {
    Map<String, CommandInfo> asUnmodifiableMap();

    T register(CommandInfo info);

    default T register(CommandInfo info1, CommandInfo info2, CommandInfo... infos) {
        final var self = register(info1); register(info2);
        for (CommandInfo info : infos) {
            register(info);
        }

        return self;
    }

    T alias(String newName, String oldName);

    default boolean containsPrefix(String prefix) {
        return asUnmodifiableMap().containsKey(prefix);
    }

    default boolean containsPrefix(CommandMessage msg) {
        return msg.getCommandPrefix().map(this::containsPrefix).orElse(false);
    }

    default Optional<CommandInfo> get(String name) {
        return Optional.ofNullable(asUnmodifiableMap().get(name));
    }

    default Optional<CommandInfo> get(CommandMessage msg) {
        return msg.getCommandPrefix().flatMap(this::get);
    }

    default Set<String> prefixes() {
        return asUnmodifiableMap().keySet();
    }

    default Collection<CommandInfo> values() {
        return asUnmodifiableMap().values();
    }
}
