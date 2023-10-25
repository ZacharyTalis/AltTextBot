package com.zacharytalis.alttextbot.slashCommands.impl;

import com.zacharytalis.alttextbot.commands.runners.IPingProvider;
import com.zacharytalis.alttextbot.commands.runners.PingRunner;
import com.zacharytalis.alttextbot.messages.UserCommandMessage;
import com.zacharytalis.alttextbot.slashCommands.SlashCommandHandler;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandBuilder;

import java.util.concurrent.CompletableFuture;

public class PingCommandHandler implements SlashCommandHandler {
    private record Provider(User user) implements IPingProvider {
    }

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
    public CompletableFuture<?> receive(UserCommandMessage.Slash command) {
        final var interaction = command.interaction();
        final var runner = new PingRunner(new Provider(interaction.getUser()));

        return interaction.createImmediateResponder()
            .setContent(runner.getPong())
            .setAllowedMentions(
                new AllowedMentionsBuilder().addUser(interaction.getUser().getId()).build()
            )
            .setFlags(MessageFlag.EPHEMERAL)
            .respond();
    }
}
