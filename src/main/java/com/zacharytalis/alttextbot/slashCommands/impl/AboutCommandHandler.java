package com.zacharytalis.alttextbot.slashCommands.impl;

import com.zacharytalis.alttextbot.bots.DiscordBot;
import com.zacharytalis.alttextbot.commands.runners.AboutRunner;
import com.zacharytalis.alttextbot.commands.runners.IAboutProvider;
import com.zacharytalis.alttextbot.messages.UserCommandMessage;
import com.zacharytalis.alttextbot.slashCommands.SlashCommandHandler;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.interaction.SlashCommandBuilder;

import java.util.concurrent.CompletableFuture;

public class AboutCommandHandler implements SlashCommandHandler {
    private record Provider(UserCommandMessage.Slash cmd) implements IAboutProvider {

        @Override
        public DiscordApi api() {
            return cmd().interaction().getApi();
        }

        @Override
        public DiscordBot<?> bot() {
            return cmd().bot();
        }
    }

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
    public CompletableFuture<?> receive(UserCommandMessage.Slash slashCommand) {
        final var runner = new AboutRunner(new Provider(slashCommand));
        final var interaction = slashCommand.interaction();

        return runner.getAboutText().thenCompose(aboutText -> {
            return interaction.createImmediateResponder().setContent(
                aboutText
            ).setFlags(MessageFlag.EPHEMERAL).respond();
        });
    }
}
