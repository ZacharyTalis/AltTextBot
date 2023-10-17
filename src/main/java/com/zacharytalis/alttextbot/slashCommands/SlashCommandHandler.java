package com.zacharytalis.alttextbot.slashCommands;

import com.zacharytalis.alttextbot.bots.DiscordBotInfo;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.concurrent.CompletableFuture;

public interface SlashCommandHandler {

    String name();

    String description();

    SlashCommandBuilder definition();

    CompletableFuture<?> receive(DiscordBotInfo botInfo, SlashCommandInteraction interaction);
}
