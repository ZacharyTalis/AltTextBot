package com.zacharytalis.alttextbot.utils;

import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.Nameable;
import org.javacord.api.entity.message.Message;

import java.util.Optional;
import java.util.function.Supplier;

public class Messages {
    public static CommandMessage asCommandMessage(final Message msg) {
        if (msg instanceof CommandMessage cmdMsg)
            return cmdMsg;

        return new CommandMessage(msg);
    }

    public static <T extends Nameable> String getNameOrElse(final Supplier<Optional<T>> getter, final String orElse) {
        return getter.get().map(Nameable::getName).orElse(orElse);
    }

    public static <T extends DiscordEntity & Nameable> String getNamedIdentifierOrElse(final Supplier<Optional<T>> getter, final String orElse) {
        return getter.get().map((t) -> String.format("%s [%s]", t.getName(), t.getIdAsString())).orElse(orElse);
    }
}
