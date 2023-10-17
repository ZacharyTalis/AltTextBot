package com.zacharytalis.alttextbot.slashCommands.impl;

import com.zacharytalis.alttextbot.bots.DiscordBotInfo;
import com.zacharytalis.alttextbot.services.LeaderboardService;
import com.zacharytalis.alttextbot.slashCommands.SlashCommandHandler;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.concurrent.CompletableFuture;

public class BoardCommandHandler implements SlashCommandHandler {
    static final int TOP_N = 5;

    @Override
    public String name() {
        return "board";
    }

    @Override
    public String description() {
        return "Show alt-text leaderboard for server";
    }

    @Override
    public SlashCommandBuilder definition() {
        return new SlashCommandBuilder()
            .setName(this.name())
            .setDescription(this.description())
            .setEnabledInDms(false)
            .setDefaultEnabledForEveryone();
    }

    @Override
    public CompletableFuture<?> receive(DiscordBotInfo botInfo, SlashCommandInteraction interaction) {
        final var server = interaction.getServer().orElseThrow();
        final var service = new LeaderboardService(server);

        return interaction
            .createImmediateResponder()
            .addEmbed(service.getLeaderboardEmbed(TOP_N))
            .respond();
    }
}
