package com.zacharytalis.alttextbot.commands.impl;

import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.commands.BaseCommandBody;
import com.zacharytalis.alttextbot.commands.CommandInfo;
import com.zacharytalis.alttextbot.utils.*;
import com.zacharytalis.alttextbot.utils.functions.Functions;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;

import java.util.stream.Collectors;

public class AboutCommand extends BaseCommandBody {
    public static CommandInfo description() {
        return new CommandInfo(
            "!atbabout",
            "Get AltTextBot's version and authorship info about in a direct message.",
            AboutCommand::new
        );
    }

    public AboutCommand(AltTextBot bot) {
        super(bot);
    }

    @Override
    public CommandInfo getInfo() {
        return AboutCommand.description();
    }

    @Override
    protected void call(CommandMessage msg) {
        bot().whenApiAvailable(api -> {
            final var botUser = api.getYourself();

            final var authorWithUsers =
                    Ref.authors.stream()
                        .map(author -> author.withUser(api))
                        .collect(Collectors.toList());

            Futures.allOf(authorWithUsers).thenAccept(authors -> {
                final var names = authors.map(author -> author.name()  + " (" + author.user().getDiscriminatedName() + ")");

                new MessageBuilder()
                    .append("Hello! I'm ")
                    .append(botUser.getNicknameMentionTag())
                    .append(" and I'm running on ")
                    .append(bot().internalName() + " v" + Ref.BOT_VERSION, MessageDecoration.BOLD, MessageDecoration.UNDERLINE)
                    .append(".")
                    .appendNewLine()
                    .append("I was created by ")
                    .append(Inflections.join(names.iterator()))
                    .append(" and my code can be found at ")
                    .append(Ref.GITHUB_REPO)
                    .append(".")
                    .send(msg.getChannel())
                        .exceptionally(Functions.partialConsumer(this::handleSendFailure, msg));
            });
        });
    }

    private void handleSendFailure(CommandMessage recv, Throwable t) {
        var author = new MessageAuthorInfo(recv.getAuthor());

        logger().error(t, "Failed to send about message. {}", recv);

        author.asUser().ifPresent(
            user -> new MessageBuilder()
                .append("Sorry ")
                .append(user.getMentionTag())
                .append(", I could not send a response to your !atbabout request in ")
                .append(DiscordEntities.getNameOrElse(recv::getServerTextChannel, "a channel"))
                .append(" in ")
                .append(DiscordEntities.getNameOrElse(recv::getServer, "a server"))
                .append("! Do I have the right permissions?")
                .send(user)
        );
    }
}
