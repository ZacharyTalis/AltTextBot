package com.zacharytalis.alttextbot.bangCommands.impl;

import com.zacharytalis.alttextbot.bangCommands.BaseCommandBody;
import com.zacharytalis.alttextbot.bangCommands.CommandInfo;
import com.zacharytalis.alttextbot.bangCommands.CommandMessage;
import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.bots.DiscordBot;
import com.zacharytalis.alttextbot.commands.runners.AboutRunner;
import com.zacharytalis.alttextbot.commands.runners.IAboutProvider;
import com.zacharytalis.alttextbot.utils.DiscordEntities;
import com.zacharytalis.alttextbot.utils.MessageAuthorInfo;
import com.zacharytalis.alttextbot.utils.functions.Functions;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageBuilder;

public class AboutCommand extends BaseCommandBody {
    private record Provider(CommandMessage msg, DiscordBot<?> bot) implements IAboutProvider {
        @Override
        public DiscordApi api() {
            return msg().getApi();
        }
    }

    public static CommandInfo description() {
        return new CommandInfo(
            "atbabout",
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
    protected void receive(CommandMessage msg) {
        bot().whenApiAvailable(api -> {
            final var runner = new AboutRunner(new Provider(msg, bot()));

            runner.getAboutText().thenCompose(aboutText ->
                msg.getChannel().sendMessage(aboutText)
            ).exceptionally(Functions.partialConsumer(this::handleSendFailure, msg));
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
