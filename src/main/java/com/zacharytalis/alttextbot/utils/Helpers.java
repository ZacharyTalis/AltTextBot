package com.zacharytalis.alttextbot.utils;

import org.javacord.api.entity.Nameable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Helpers {
    public static <T, U extends String> U getDiscordName(Optional<T> optional, Function<T, U> nameGetter, Supplier<U> orElse) {
        return optional.map(nameGetter).orElseGet(orElse);
    }

    public static <T extends Nameable> String getDiscordName(Optional<T> optional, Supplier<String> orElse) {
        return getDiscordName(optional, Nameable::getName, orElse);
    }
}
