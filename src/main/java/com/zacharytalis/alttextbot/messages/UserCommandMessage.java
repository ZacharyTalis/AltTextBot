package com.zacharytalis.alttextbot.messages;

import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import org.javacord.api.interaction.SlashCommandInteraction;

public interface UserCommandMessage {
    record Slash(AltTextBot bot, SlashCommandInteraction interaction) implements UserCommandMessage {
    }

    record Bang(AltTextBot bot, CommandMessage commandMessage) implements UserCommandMessage {
    }
}
