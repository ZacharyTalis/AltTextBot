package com.zacharytalis.alttextbot.bangCommands.registry;

import com.zacharytalis.alttextbot.bangCommands.CommandInfo;
import com.zacharytalis.alttextbot.utils.ReadOnly;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class CommandRegistry implements ICommandRegistry<CommandRegistry>, ReadOnly<ICommandRegistry<CommandRegistry>> {
    private final Map<String, CommandInfo> commandMap = new LinkedHashMap<>();
    private final Map<String, CommandInfo> unmodifiableMap = Collections.unmodifiableMap(commandMap);

    @Override
    public ICommandRegistry<CommandRegistry> readOnly() {
        return new ReadOnlyCommandRegistry<>(this);
    }

    @Override
    public Map<String, CommandInfo> asUnmodifiableMap() {
        return unmodifiableMap;
    }

    @Override
    public CommandRegistry register(CommandInfo info) {
        commandMap.put(info.name(), info);
        return this;
    }

    @Override
    public CommandRegistry alias(String newName, String oldName) {
        final var oldInfo = get(oldName).orElseThrow(
            () -> new NoSuchElementException(oldName + " does not exist in the registry to alias")
        );

        final var newInfo = new CommandInfo(
            newName,
            oldInfo.helpInfo(),
            oldInfo.factory()
        );

        return register(newInfo);
    }
}
