package com.zacharytalis.alttextbot.utils;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Icon;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.user.User;

import java.util.Optional;

public record MessageAuthorInfo(MessageAuthor author) implements MessageAuthor, Loggable {
    @Override
    public boolean equals(Object obj) {
        return author.equals(obj);
    }

    @Override
    public int hashCode() {
        return author.hashCode();
    }

    @Override
    public String toString() {
        return author.toString();
    }

    @Override
    public Message getMessage() {
        return author.getMessage();
    }

    @Override
    public Optional<String> getDiscriminator() {
        return author.getDiscriminator();
    }

    @Override
    public Icon getAvatar() {
        return author.getAvatar();
    }

    public boolean isYourself() {
        return asUser().map(User::isYourself).orElse(false);
    }

    @Override
    public boolean isUser() {
        return author.isUser();
    }

    @Override
    public boolean isWebhook() {
        return author.isWebhook();
    }

    @Override
    public DiscordApi getApi() {
        return author.getApi();
    }

    @Override
    public long getId() {
        return author.getId();
    }

    @Override
    public String getName() {
        return author.getName();
    }

    @Override
    public String toLoggerString() {
        return String.format(
            "author: %s, is_webhook: %s, is_bot: %s, is_self: %s",
            getDiscriminatedName(),
            toYesNo(isWebhook()),
            toYesNo(isBotUser()),
            toYesNo(isYourself())
        );
    }

    @Override
    public Optional<User> asUser() {
        return author.asUser();
    }
}
