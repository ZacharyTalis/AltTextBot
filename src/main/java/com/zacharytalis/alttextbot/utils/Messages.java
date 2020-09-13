package com.zacharytalis.alttextbot.utils;

import com.zacharytalis.alttextbot.utils.functions.Suppliers;
import org.javacord.api.entity.Nameable;
import org.javacord.api.entity.message.Message;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class Messages {
    public static CommandMessage asCommandMessage(final Message msg) {
        if (msg instanceof CommandMessage)
            return (CommandMessage) msg;

        return new CommandMessage(msg);
    }

    public static <T extends Nameable> String getNameOrElse(final Message msg, final Function<Message, Optional<T>> getter, final String orElse) {
        return getNameOrElse(() -> getter.apply(msg), orElse);
    }

    public static <T extends Nameable> String getNameOrElse(final Supplier<Optional<T>> getter, final String orElse) {
        return Helpers.getDiscordName(getter.get(), Nameable::getName, Suppliers.supplying(orElse));
    }
}
