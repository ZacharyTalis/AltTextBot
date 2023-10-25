package com.zacharytalis.alttextbot.slashCommands.impl;

import com.zacharytalis.alttextbot.messages.UserCommandMessage;
import com.zacharytalis.alttextbot.services.AltTextContributionService;
import com.zacharytalis.alttextbot.slashCommands.SlashCommandHandler;
import com.zacharytalis.alttextbot.values.AltTextEntry;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.TextInput;
import org.javacord.api.entity.message.component.TextInputStyle;
import org.javacord.api.interaction.InteractionBase;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class AltCommandHandler implements SlashCommandHandler {
    public static class InvalidModalResponseException extends RuntimeException {
        InvalidModalResponseException(String msg) {
            super(msg);
        }
    }

    private static final String ALT_TEXT_FIELD = "alt_text";

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
    public CompletableFuture<?> receive(UserCommandMessage.Slash command) {
        final var interaction = command.interaction();
        final var modalId = this.uniqueId(interaction);

        listenForModalSubmit(interaction, modalId)
            .thenAccept(this::registerAltContribution);

        return interaction.respondWithModal(
            modalId,
            "Image Alt Text",
            ActionRow.of(
                TextInput.create(
                    TextInputStyle.PARAGRAPH,
                    ALT_TEXT_FIELD,
                    "Alt Text"
                )
            )
        );
    }

    private CompletableFuture<AltTextEntry> listenForModalSubmit(SlashCommandInteraction interaction, String modalId) {
        final var respondedFuture = new CompletableFuture<AltTextEntry>();

        final var listener = interaction.getApi().addModalSubmitListener(event -> {
            final var modal = event.getModalInteraction();

            if (modal.getCustomId().equals(modalId)) {
                modal.getTextInputValueByCustomId(ALT_TEXT_FIELD).ifPresentOrElse(
                    altText -> {
                        modal.getChannel().ifPresent(channel -> {
                            modal.createImmediateResponder()
                                .setContent(altText)
                                .respond()
                                .thenRun(() -> respondedFuture.complete(
                                    new AltTextEntry(
                                        modal.getUser(),
                                        modal.getServer().orElseThrow(),
                                        altText
                                    )
                                ));
                        });
                    },
                    () -> respondedFuture.completeExceptionally(
                        new InvalidModalResponseException("Missing value for " + ALT_TEXT_FIELD + " in modal " + modalId)
                    )
                );
            }
        }).removeAfter(30, TimeUnit.MINUTES);

        respondedFuture
            .whenComplete((_i, _ex) -> listener.remove());

        return respondedFuture;
    }

    private String uniqueId(InteractionBase interaction) {
        final var user = interaction.getUser().getName();
        final var randomValue = ThreadLocalRandom.current().nextLong();

        return "alt-" + user + "-" + randomValue;
    }

    private void registerAltContribution(AltTextEntry entry) {
        final var service = new AltTextContributionService(entry.server());

        service.increment(entry.user());
    }
}
