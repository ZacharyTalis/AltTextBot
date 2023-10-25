package com.zacharytalis.alttextbot.commands.runners;

import org.javacord.api.entity.message.MessageBuilder;

public class PingRunner {
    private final IPingProvider provider;

    public PingRunner(IPingProvider provider) {
        this.provider = provider;
    }

    public String getPong() {
        return new MessageBuilder()
            .append("Yes, yes I'm here and alive ")
            .append(provider.user())
            .append(" - PONG")
            .getStringBuilder()
            .toString();
    }
}
