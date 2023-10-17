package com.zacharytalis.alttextbot.slashCommands.impl;

import com.zacharytalis.alttextbot.bots.DiscordBotInfo;
import com.zacharytalis.alttextbot.slashCommands.SlashCommandHandler;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.mention.AllowedMentions;
import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.concurrent.CompletableFuture;

public class PingCommandHandler implements SlashCommandHandler {
    @Override
    public String name() {
        return "ping";
    }

    @Override
    public String description() {
        return "Check if the bot is alive, it will respond annoyingly";
    }

    @Override
    public SlashCommandBuilder definition() {
        return new SlashCommandBuilder()
            .setName(this.name())
            .setDescription(this.description())
            .setEnabledInDms(true)
            .setDefaultEnabledForEveryone();
    }

    @Override
    public CompletableFuture<?> receive(DiscordBotInfo botInfo, SlashCommandInteraction interaction) {
        return interaction.createImmediateResponder()
            .setContent(
                new MessageBuilder()
                    .append("Yes, yes I'm here and alive ")
                    .append(interaction.getUser())
                    .append(" - PONG")
                    .getStringBuilder()
                    .toString()
            )
            .setAllowedMentions(
                new AllowedMentionsBuilder().addUser(interaction.getUser().getId()).build()
            )
            .setFlags(MessageFlag.EPHEMERAL)
            .respond();
    }
}
