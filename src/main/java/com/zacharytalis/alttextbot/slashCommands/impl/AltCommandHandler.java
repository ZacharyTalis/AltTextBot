package com.zacharytalis.alttextbot.slashCommands.impl;

import com.zacharytalis.alttextbot.bots.DiscordBotInfo;
import com.zacharytalis.alttextbot.services.AltTextContributionService;
import com.zacharytalis.alttextbot.slashCommands.SlashCommandHandler;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.TextInput;
import org.javacord.api.entity.message.component.TextInputStyle;
import org.javacord.api.interaction.InteractionBase;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AltCommandHandler implements SlashCommandHandler {
    private final Random random = new Random();

    @Override
    public String name() {
        return "alt";
    }

    @Override
    public String description() {
        return "Set an alt text for an image";
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
        final var customId = this.uniqueId(interaction);
        final var respondedFuture = new CompletableFuture<InteractionBase>();

        final var listener = interaction.getApi().addModalSubmitListener(event -> {
            final var modal = event.getModalInteraction();

            if (modal.getCustomId().equals(customId)) {
                final var altText = modal.getTextInputValueByCustomId("alt_text").get();
                modal.getChannel().ifPresent(channel -> {
                    modal.createImmediateResponder()
                        .setContent(altText)
                        .respond()
                        .thenRun(() -> respondedFuture.complete(modal));
                });
            }
        }).removeAfter(30, TimeUnit.MINUTES);

        respondedFuture.thenRun(listener::remove);
        respondedFuture.thenAccept(this::registerAltContribution);

        return interaction.respondWithModal(
            customId,
            "Image Alt Text",
            ActionRow.of(
                TextInput.create(
                    TextInputStyle.PARAGRAPH,
                    "alt_text",
                    "Alt Text"
                )
            )
        );
    }

    private String uniqueId(InteractionBase interaction) {
        final var user = interaction.getUser().getName();
        final var randomValue = random.nextLong();

        return "alt-" + user + "-" + randomValue;
    }

    private void registerAltContribution(InteractionBase interaction) {
        final var user = interaction.getUser();
        final var server = interaction.getServer().orElseThrow();
        final var service = new AltTextContributionService(server);

        service.increment(user);
    }
}
