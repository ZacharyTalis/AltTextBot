package com.zacharytalis.alttextbot.commands.impl;

import com.zacharytalis.alttextbot.board.BoardServerFile;
import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.commands.BaseCommandBody;
import com.zacharytalis.alttextbot.commands.CommandInfo;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.MessageAuthorInfo;
import com.zacharytalis.alttextbot.utils.Messages;
import org.javacord.api.entity.message.MessageBuilder;

import static com.zacharytalis.alttextbot.utils.functions.Functions.partial;

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

        // Try sending message before deleting the users submission, otherwise we might delete their work *and*
        // not post anything which would be :(
        recv.getChannel().sendMessage(altText)
            .thenAcceptAsync(sentMsg -> {
               recv.delete("Alt-text submission")
                   .exceptionally(
                       partial(this::handleDeletionFailure, recv)
                   );
               new BoardServerFile(recv.getServerID()).incrementUserScore(recv.getUserAuthor().get().getId());
            })
            .exceptionallyAsync(
                partial(this::handleSendFailure, recv)
            );

        // User submitted alt-text, so increment their score.

    }

    private String getAltContent(CommandMessage msg) {
        return msg.getContent().substring(getName().length()).trim();
    }

    private void handleDeletionFailure(CommandMessage recv, Throwable t) {
        var author = new MessageAuthorInfo(recv.getAuthor());

        logger().error(t, "Failed to delete message. {}", recv);

        author.asUser().ifPresent(
            user -> {
                new MessageBuilder()
                    .append("Sorry ")
                    .append(user.getMentionTag())
                    .append(", I could not delete your !alt message in ")
                    .append(Messages.getNameOrElse(recv::getServerTextChannel, "a channel"))
                    .append(" in ")
                    .append(Messages.getNameOrElse(recv::getServer, "a server"))
                    .append("! Do I have the right permissions?")
                    .send(user);
            }
        );
    }

    private void handleSendFailure(CommandMessage recv, Throwable t) {
        var author = new MessageAuthorInfo(recv.getAuthor());

        logger().error(t, "Failed to send alt text message. {}", recv);

        author.asUser().ifPresentOrElse(
            user -> {
                new MessageBuilder()
                    .append("Sorry ")
                    .append(user.getMentionTag())
                    .append(", I could not send your !alt message in ")
                    .append(Messages.getNameOrElse(recv::getServerTextChannel, "a channel"))
                    .append(" in ")
                    .append(Messages.getNameOrElse(recv::getServer, "a server"))
                    .append("! Do I have the right permissions?")
                    .send(user);
            },
            () -> {
                new MessageBuilder()
                    .append("Sorry ")
                    .append(author.getDisplayName())
                    .append(", I could not send your !alt message! Do I have the right permissions?")
                    .send(recv.getChannel());
            }
        );
    }
}
