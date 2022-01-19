package com.zacharytalis.alttextbot.commands.impl;

import com.zacharytalis.alttextbot.board.v2.AltTextContribution;
import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.commands.BaseCommandBody;
import com.zacharytalis.alttextbot.commands.CommandInfo;
import com.zacharytalis.alttextbot.db.ConnectionPool;
import com.zacharytalis.alttextbot.db.dao.AltTextContributionDao;
import com.zacharytalis.alttextbot.db.dao.ServerDao;
import com.zacharytalis.alttextbot.db.dao.UserDao;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.DiscordEntities;
import com.zacharytalis.alttextbot.utils.MessageAuthorInfo;
import org.javacord.api.entity.message.MessageBuilder;

import static com.zacharytalis.alttextbot.utils.functions.Functions.partialConsumer;

public class AltCommand extends BaseCommandBody {
    public static CommandInfo description() {
        return new CommandInfo(
            "!alt",
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
    protected void call(CommandMessage recv) {
        final var altText = getAltContent(recv);

        // First, send alt-text submission
        // After sending, attempt to increment score and delete simultaneously

        // Send
        final var sendFuture =
                recv.getChannel().sendMessage(altText);
        sendFuture.exceptionally(partialConsumer(this::handleSendFailure, recv));

        // Increment once sent successfully
        sendFuture.thenRunAsync(() -> {
            final var userDiscordId = recv.getUserAuthor().orElseThrow().getId();
            final var serverDiscordId = recv.getServerID();

            ConnectionPool.useHandle(handle -> {
               final var ud = handle.attach(UserDao.class);
               final var sd = handle.attach(ServerDao.class);
               final var atcd = handle.attach(AltTextContributionDao.class);

               final var user = ud.fetchOrCreate(userDiscordId);
               final var server = sd.fetchOrCreate(serverDiscordId);
               final var contrib = atcd.fetchOrCreate(user, server);
               final var newScore = atcd.increment(contrib);

               final var serverName = DiscordEntities.getNamedIdentifierOrElse(recv::getServer, "<unknown>");

               logger().info(
                   "({}) in {} incremented score from {} to {}",
                   recv.getAuthorInfo(), serverName, contrib.score(), newScore.score()
               );
            });
        }).exceptionally(partialConsumer(this::handleIncrementFailure, recv));

        // Delete once sent successfully
        sendFuture
            .thenComposeAsync(sentMsg -> recv.delete("Alt-text submission"))
            .exceptionally(partialConsumer(this::handleDeletionFailure, recv));
    }

    private String getAltContent(CommandMessage msg) {
        return msg.getContent().substring(getName().length()).trim();
    }

    private void handleDeletionFailure(CommandMessage recv, Throwable t) {
        var author = new MessageAuthorInfo(recv.getAuthor());

        logger().error(t, "Failed to delete message. {}", recv);

        author.asUser().ifPresent(
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
        var author = recv.getAuthorInfo();

        logger().error(t, "Failed to send alt text message. {}", recv);

        author.asUser().ifPresentOrElse(
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
                .append(author.getDisplayName())
                .append(", I could not send your !alt message! Do I have the right permissions?")
                .send(recv.getChannel())
        );
    }

    private void handleIncrementFailure(CommandMessage recv, Throwable t) {
        logger().error(t, "Failed to increment score for alt-text. {}", recv);
    }
}
