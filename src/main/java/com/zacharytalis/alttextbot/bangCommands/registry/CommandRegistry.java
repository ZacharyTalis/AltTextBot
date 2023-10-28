package com.zacharytalis.alttextbot.bangCommands.registry;

import com.google.common.collect.ImmutableMap;
import com.zacharytalis.alttextbot.bangCommands.CommandInfo;
import com.zacharytalis.alttextbot.utils.ReadOnly;

import java.util.*;

public class CommandRegistry implements ICommandRegistry<CommandRegistry>, ReadOnly<ICommandRegistry<CommandRegistry>> {
    private final Map<String, CommandInfo> commandMap;

    public CommandRegistry() {
        this.commandMap = new LinkedHashMap<>();
    }

    private CommandRegistry(Map<String, CommandInfo> map) {
        this.commandMap = map;
    }

    @Override
    public ICommandRegistry<CommandRegistry> readOnly() {
        return new CommandRegistry(ImmutableMap.copyOf(this.commandMap));
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

    @Override
    public boolean containsPrefix(String prefix) {
        return this.commandMap.containsKey(prefix);
    }

    @Override
    public Optional<CommandInfo> get(String name) {
        return Optional.ofNullable(this.commandMap.get(name));
    }

    @Override
    public Collection<CommandInfo> values() {
        return Collections.unmodifiableCollection(this.commandMap.values());
    }
}
