package com.zacharytalis.alttextbot.slashCommands.impl;

import com.zacharytalis.alttextbot.bots.DiscordBotInfo;
import com.zacharytalis.alttextbot.slashCommands.SlashCommandHandler;
import com.zacharytalis.alttextbot.utils.Futures;
import com.zacharytalis.alttextbot.utils.Inflections;
import com.zacharytalis.alttextbot.utils.Ref;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AboutCommandHandler implements SlashCommandHandler {
    @Override
    public String name() {
        return "about";
    }

    @Override
    public String description() {
        return "Information about Alt Text Bot";
    }

    @Override
    public SlashCommandBuilder definition() {
        return new SlashCommandBuilder()
            .setName(this.name())
            .setDescription(this.description())
            .setDefaultEnabledForEveryone();
    }

    @Override
    public CompletableFuture<?> receive(DiscordBotInfo botInfo, SlashCommandInteraction interaction) {
        final var api = interaction.getApi();
        final var botUser = api.getYourself();

        return authorsAsync(api).thenCompose(authors -> {
            final var names = authors.stream().map(author -> author.name() + " (@" + author.user().getName() + ")");

            return interaction.createImmediateResponder().setContent(
                new MessageBuilder()
                    .append("Hello! I'm ")
                    .append(botUser.getNicknameMentionTag())
                    .append(" and I'm running on ")
                    .append(botInfo.internalName() + " v" + botInfo.version(), MessageDecoration.BOLD, MessageDecoration.UNDERLINE)
                    .append(".")
                    .appendNewLine()
                    .append("I was created by ")
                    .append(Inflections.join(names.iterator()))
                    .append(" and my code can be found at ")
                    .append(Ref.GITHUB_REPO)
                    .append(".")
                    .getStringBuilder().toString()
            ).setFlags(MessageFlag.EPHEMERAL).respond();
        });
    }

    private CompletableFuture<List<Ref.ProjectAuthor.ProjectAuthorWithUser>> authorsAsync(DiscordApi api) {
        return Futures.lift(Ref.authors.stream().map(author -> author.withUser(api)).collect(Collectors.toList()));
    }
}
