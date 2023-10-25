package com.zacharytalis.alttextbot.messages;

import com.zacharytalis.alttextbot.bangCommands.CommandMessage;
import com.zacharytalis.alttextbot.bots.AltTextBot;
import org.javacord.api.interaction.SlashCommandInteraction;

public interface UserCommandMessage {
    record Slash(AltTextBot bot, SlashCommandInteraction interaction) implements UserCommandMessage {
    }

    record Bang(AltTextBot bot, CommandMessage commandMessage) implements UserCommandMessage {
    }

    AltTextBot bot();

    @SuppressWarnings("unchecked")
    default <U extends UserCommandMessage> U coerce() {
        return (U) this;
    }
}
