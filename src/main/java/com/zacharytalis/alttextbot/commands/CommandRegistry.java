package com.zacharytalis.alttextbot.commands;

import com.google.common.collect.ForwardingMap;
import com.zacharytalis.alttextbot.DiscordAPI;
import com.zacharytalis.alttextbot.entities.message.Message;
import com.zacharytalis.alttextbot.utils.ReadOnly;
import com.zacharytalis.alttextbot.utils.functions.TriConsumer;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandRegistry extends ForwardingMap<String, ICommand> implements ReadOnly<CommandRegistry> {
    static class ReadOnlyCommandRegistry extends CommandRegistry {
        public ReadOnlyCommandRegistry(CommandRegistry registry) {
            this.commands = Collections.unmodifiableMap(registry.commands);
        }
    }

    public Map<String, ICommand> commands;

    public CommandRegistry() {
        commands = new ConcurrentHashMap<>();
    }

    public CommandRegistry register(String name, String info, TriConsumer<ReadOnly<CommandRegistry>, DiscordAPI, Message> action) {
        return register(new ICommand() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getInfo() {
                return info;
            }

            @Override
            public void execute(ReadOnly<CommandRegistry> registry, DiscordAPI api, Message msg) {
                action.accept(registry, api, msg);
            }
        });
    }

    public CommandRegistry register(ICommand command) {
        commands.put(command.getName(), command);
        return this;
    }

    public CommandRegistry register(ICommand ...commands)
    {
        for (ICommand cmd: commands)
            register(cmd);

        return this;
    }

    public CommandRegistry alias(String alias, String originalName) {
        ICommand command = commands.get(originalName);

        if (command == null)
            throw new NullPointerException("Command using " + originalName + " is not registered.");

        put(alias, command);
        return this;
    }

    public ReadOnlyCommandRegistry readOnly() {
        return new ReadOnlyCommandRegistry(this);
    }

    public ICommand get(Message msg) {
        String prefix = msg.getPrefix();
        return this.get(prefix);
    }

    public ICommand get(String name) {
        return commands.get(name);
    }

    @Override
    protected Map<String, ICommand> delegate() {
        return commands;
    }

    @Override
    public ICommand get(Object key) {
        throw new ClassCastException("Key must be string or message, got type of: " + key.getClass().getName());
    }

    @Override
    public ICommand put(String key, ICommand value) {
        this.register(key, value.getInfo(), value::execute);
        return value;
    }

    public ICommand remove(ICommand key) {
        return super.remove(key.getName());
    }

    public boolean containsKey(Message msg) {
        return containsKey(msg.getPrefix());
    }

    @Override
    public ICommand remove(Object key) {
        if (key.getClass() == String.class)
            return super.remove(key);
        else if (ICommand.class.isAssignableFrom(key.getClass()))
            return remove((ICommand) key);


        throw new ClassCastException("Key must be string, got type of: " + key.getClass().getName());
    }

    @Override
    public void putAll(Map<? extends String, ? extends ICommand> m) {
        if (CommandRegistry.class.isAssignableFrom(m.getClass()))
            commands.putAll(((CommandRegistry) m).commands);
        else
            super.putAll(m);
    }
}
