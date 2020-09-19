package com.zacharytalis.alttextbot.commands.impl;

import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.commands.BaseCommandBody;
import com.zacharytalis.alttextbot.commands.CommandInfo;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.functions.Functions;
import org.javacord.api.entity.message.MessageBuilder;

public class HelpCommand extends BaseCommandBody {
    public static CommandInfo description() {
        return new CommandInfo(
            "!atbhelp",
            "Get all commands from AltTextBot in a direct message.",
            HelpCommand::new
        );
    }

    public HelpCommand(AltTextBot bot) {
        super(bot);
    }

    @Override
    public CommandInfo getInfo() {
        return HelpCommand.description();
    }

    @Override
    protected void call(CommandMessage msg) {
        MessageBuilder helpText =
                bot()
                .commands()
                .readOnly()
                .values()
                .stream()
                .reduce(
                    new MessageBuilder(),
                    (var mb, var cmd) -> mb.append(cmd.name()).append(" ~ ").append(cmd.helpInfo()).appendNewLine(),
                    MessageBuilder::append
                );

        msg.getUserAuthor().map(helpText::send).ifPresent(msgFuture -> {
            msgFuture.exceptionally(Functions.nullify(t -> {
                logger().error(t, "Failed to send help text to user. {}", msg);
            }));
        });
    }
}
