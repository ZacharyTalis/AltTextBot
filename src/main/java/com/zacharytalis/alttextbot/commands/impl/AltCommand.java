package com.zacharytalis.alttextbot.commands.impl;

import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.commands.BaseCommandBody;
import com.zacharytalis.alttextbot.commands.CommandInfo;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.Messages;
import org.javacord.api.entity.message.Message;
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
            })
            .exceptionallyAsync(
                partial(this::handleSendFailure, recv)
            );
    }

    private String getAltContent(Message msg) {
        return msg.getContent().substring(getName().length()).trim();
    }

    private void handleDeletionFailure(Message recv, Throwable t) {
        var authorUsername = recv.getAuthor().getDiscriminatedName();
        var authorDisplay = recv.getAuthor().getDisplayName();

        getLogger().error("Failed to delete message for " + authorUsername, t);

        recv.getUserAuthor().ifPresent(
            user -> {
                new MessageBuilder()
                    .append("Sorry ")
                    .append(authorDisplay)
                    .append(", I could not delete your !alt message in ")
                    .append(Messages.getNameOrElse(recv::getServerTextChannel, "a channel"))
                    .append(" in ")
                    .append(Messages.getNameOrElse(recv::getServer, "a server"))
                    .append("! Do I have the right permissions?")
                    .send(user);
            }
        );
    }

    private void handleSendFailure(Message recv, Throwable t) {
        var authorUsername = recv.getAuthor().getDiscriminatedName();
        var authorDisplay = recv.getAuthor().getDisplayName();
        var channel = Messages.getNameOrElse(recv::getServerTextChannel, "<unknown>");
        var server = Messages.getNameOrElse(recv::getServer, "<unknown>");

        var errorMsg = String.format("Failed to send alt text message. user: %s, channel: %s, server: %s", authorUsername, channel, server);
        getLogger().error(errorMsg, t);

        recv.getUserAuthor().ifPresentOrElse(
            user -> {
                new MessageBuilder()
                    .append("Sorry ")
                    .append(authorDisplay)
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
                    .append(authorDisplay)
                    .append(", I could not send your !alt message! Do I have the right permissions?")
                    .send(recv.getChannel());
            }
        );
    }
}
