package com.zacharytalis.alttextbot.bangCommands.impl;

import com.zacharytalis.alttextbot.bangCommands.BaseCommandBody;
import com.zacharytalis.alttextbot.bangCommands.CommandInfo;
import com.zacharytalis.alttextbot.bangCommands.CommandMessage;
import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.services.AltTextContributionService;
import com.zacharytalis.alttextbot.utils.DiscordEntities;
import org.javacord.api.entity.message.MessageBuilder;

import static com.zacharytalis.alttextbot.utils.functions.Functions.partialConsumer;

public class AltCommand extends BaseCommandBody {
    public static CommandInfo description() {
        return new CommandInfo(
            "alt",
            "Replace the user message with alt-text. Post your alt-text as a separate message with the " +
                "format `!alt [alt-text]` (no brackets).",
            AltCommand::new
        );
    }

    public AltCommand(AltTextBot bot) {
        super(bot);
    }

    @Override
    public CommandInfo getInfo() {
        return AltCommand.description();
    }

    @Override
    protected void receive(CommandMessage recv) {
        final var altText = getAltContent(recv);

        // First, send alt-text submission
        // After sending, attempt to increment score and delete simultaneously

        // Send
        final var sendFuture =
            recv.getChannel().sendMessage(altText);
        sendFuture.exceptionally(partialConsumer(this::handleSendFailure, recv));

        // Increment once sent successfully
        sendFuture.thenRunAsync(() -> {
            final var user = recv.getUserAuthor().orElseThrow();
            final var server = recv.getServer().orElseThrow();
            final var service = new AltTextContributionService(server);

            final var newScore = service.increment(user);

            logger().info(
                "({}) in {} incremented score to {}",
                recv.getAuthorInfo(), server.getName(), newScore
            );
        }).exceptionally(partialConsumer(this::handleIncrementFailure, recv));

        // Delete once sent successfully
        sendFuture
            .thenComposeAsync(sentMsg -> recv.delete("Alt-text submission"))
            .exceptionally(partialConsumer(this::handleDeletionFailure, recv));
    }

    private String getAltContent(CommandMessage msg) {
        return msg.getContent().substring(getCommandPrefix().length()).trim();
    }

    private void handleDeletionFailure(CommandMessage recv, Throwable t) {
        var authorInfo = recv.getAuthorInfo();

        logger().error(t, "Failed to delete message. {}", recv);

        authorInfo.authorUser().ifPresent(
            user -> new MessageBuilder()
                .append("Sorry ")
                .append(user.getMentionTag())
                .append(", I could not delete your !alt message in ")
                .append(DiscordEntities.getNameOrElse(recv::getServerTextChannel, "a channel"))
                .append(" in ")
                .append(DiscordEntities.getNameOrElse(recv::getServer, "a server"))
                .append("! Do I have the right permissions?")
                .send(user)
        );
    }

    private void handleSendFailure(CommandMessage recv, Throwable t) {
        var authorInfo = recv.getAuthorInfo();

        logger().error(t, "Failed to send alt text message. {}", recv);

        authorInfo.authorUser().ifPresentOrElse(
            user -> new MessageBuilder()
                .append("Sorry ")
                .append(user.getMentionTag())
                .append(", I could not send your !alt message in ")
                .append(DiscordEntities.getNameOrElse(recv::getServerTextChannel, "a channel"))
                .append(" in ")
                .append(DiscordEntities.getNameOrElse(recv::getServer, "a server"))
                .append("! Do I have the right permissions?")
                .send(user),

            () -> new MessageBuilder()
                .append("Sorry ")
                .append(authorInfo.messageAuthor().getDisplayName())
                .append(", I could not send your !alt message! Do I have the right permissions?")
                .send(recv.getChannel())
        );
    }

    private void handleIncrementFailure(CommandMessage recv, Throwable t) {
        logger().error(t, "Failed to increment score for alt-text. {}", recv);
    }
}
