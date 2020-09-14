package com.zacharytalis.alttextbot.commands.impl;

import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.commands.BaseCommandBody;
import com.zacharytalis.alttextbot.commands.CommandInfo;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.functions.Functions;
import org.javacord.api.entity.message.MessageBuilder;

public class PingCommand extends BaseCommandBody {
    public static CommandInfo description() {
        return new CommandInfo(
            "!atbping",
            "Check to see if AltTextBot is alive.",
            PingCommand::new
        );
    }

    public PingCommand(AltTextBot bot) {
        super(bot);
    }

    @Override
    public CommandInfo getInfo() {
        return PingCommand.description();
    }

    @Override
    protected void call(CommandMessage msg) {
        msg.getUserAuthor().ifPresent(user -> {
            new MessageBuilder()
                .append("Yes, yes, I'm here, ")
                .append(user.getMentionTag())
                .send(msg.getChannel())
                .exceptionallyAsync(Functions.nullify(t -> {
                    logger().error(t, "Failed to pong. {}", msg);
                }));
        });
    }
}
