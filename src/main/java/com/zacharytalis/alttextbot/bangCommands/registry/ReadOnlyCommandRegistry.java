package com.zacharytalis.alttextbot.bangCommands.registry;

import com.zacharytalis.alttextbot.bangCommands.CommandInfo;
import com.zacharytalis.alttextbot.utils.ReadOnly;

import java.util.Map;

public class ReadOnlyCommandRegistry<T extends ICommandRegistry<T>> implements ICommandRegistry<T>, ReadOnly<ICommandRegistry<T>> {
    private final T registry;

    public ReadOnlyCommandRegistry(final T wrapped) {
        this.registry = wrapped;
    }

    @Override
    public Map<String, CommandInfo> asUnmodifiableMap() {
        return registry.asUnmodifiableMap();
    }

    @Override
    public T register(CommandInfo info) {
        throw new ReadOnly.AttemptedWriteException("cannot register command on read only registry.");
    }

    @Override
    public T alias(String newName, String oldName) {
        throw new ReadOnly.AttemptedWriteException("cannot alias command on read only registry");
    }

    @Override
    public ICommandRegistry<T> readOnly() {
        return this;
    }
}
