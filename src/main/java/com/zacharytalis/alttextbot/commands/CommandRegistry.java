package com.zacharytalis.alttextbot.commands;

import com.google.common.collect.ForwardingMap;

import java.util.*;

public class CommandRegistry extends ForwardingMap<String, CommandInfo> implements ICommandRegistry<CommandRegistry> {
    protected final Map<String, CommandInfo> commands;

    public CommandRegistry() {
        // This could be a ConcurrentSkipListMap if we ever need concurrency guarantees,
        // may or may not sacrifice predictable iteration order.
        commands = new LinkedHashMap<>();
    }

    private CommandRegistry(Map<String, CommandInfo> commands) {
        this.commands = commands;
    }

    @Override
    public CommandRegistry readOnly() {
        return new CommandRegistry(
            Collections.unmodifiableMap(this.commands)
        );
    }

    @Override
    protected Map<String, CommandInfo> delegate() {
        return commands;
    }
}
