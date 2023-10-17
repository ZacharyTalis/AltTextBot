package com.zacharytalis.alttextbot.slashCommands;

import com.zacharytalis.alttextbot.utils.Futures;
import org.javacord.api.DiscordApi;
import org.javacord.api.interaction.ApplicationCommand;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SlashCommandHandlerCollection extends LinkedList<SlashCommandHandler> {
    public CompletableFuture<Void> setGlobalCommands(DiscordApi api) {
        final var nameSet = this.stream().map(SlashCommandHandler::name).collect(Collectors.toSet());
        final var slashCmdsFuture = api.getGlobalSlashCommands();

        final var cmdDeletion =
            slashCmdsFuture.thenComposeAsync(registeredCommands -> Futures.allOf(
                registeredCommands
                    .stream()
                    .filter(c -> !nameSet.contains(c.getName()))
                    .map(ApplicationCommand::delete)
                    .toList()
            ));

        final var cmdRegistration =
            Futures.allOf(
                this.stream()
                    .map(handler -> handler.definition().createGlobal(api))
                    .toList()
            );

        return CompletableFuture.allOf(cmdDeletion, cmdRegistration);
    }
}
