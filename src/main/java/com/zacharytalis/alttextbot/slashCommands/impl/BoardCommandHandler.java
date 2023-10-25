package com.zacharytalis.alttextbot.slashCommands.impl;

import com.zacharytalis.alttextbot.commands.runners.BoardRunner;
import com.zacharytalis.alttextbot.commands.runners.IBoardProvider;
import com.zacharytalis.alttextbot.messages.UserCommandMessage;
import com.zacharytalis.alttextbot.slashCommands.SlashCommandHandler;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.SlashCommandBuilder;

import java.util.concurrent.CompletableFuture;

public class BoardCommandHandler implements SlashCommandHandler {
    private record Provider(Server server) implements IBoardProvider {}

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
    public CompletableFuture<?> receive(UserCommandMessage.Slash command) {
        final var interaction = command.interaction();
        final var server = interaction.getServer().orElseThrow();
        final var runner = new BoardRunner(new Provider(server));

        return interaction
            .createImmediateResponder()
            .addEmbed(runner.getLeaderboardEmbed())
            .respond();
    }
}
