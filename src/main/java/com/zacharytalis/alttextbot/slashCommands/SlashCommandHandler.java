package com.zacharytalis.alttextbot.slashCommands;

import com.zacharytalis.alttextbot.messages.UserCommandMessage;
import org.javacord.api.interaction.SlashCommandBuilder;

import java.util.concurrent.CompletableFuture;

public interface SlashCommandHandler {

    String name();

    String description();

    SlashCommandBuilder definition();

    CompletableFuture<?> receive(UserCommandMessage.Slash slashCommand);
}
