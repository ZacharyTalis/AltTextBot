package com.zacharytalis.alttextbot.bangCommands.impl;

import com.zacharytalis.alttextbot.bangCommands.BaseCommandBody;
import com.zacharytalis.alttextbot.bangCommands.CommandInfo;
import com.zacharytalis.alttextbot.bangCommands.CommandMessage;
import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.commands.runners.IPingProvider;
import com.zacharytalis.alttextbot.commands.runners.PingRunner;
import com.zacharytalis.alttextbot.utils.functions.Functions;
import org.javacord.api.entity.user.User;

public class PingCommand extends BaseCommandBody {
    private record Provider(User user) implements IPingProvider {}

    public static CommandInfo description() {
        return new CommandInfo(
            "atbping",
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
    protected void receive(CommandMessage msg) {
        msg.getUserAuthor().ifPresent(user -> {
            final var runner = new PingRunner(new Provider(user));

            msg.getChannel()
                .sendMessage(runner.getPong())
                .exceptionally(Functions.partialConsumer(this::handleSendFailed, msg));
        });
    }

    private void handleSendFailed(final CommandMessage msg, final Throwable t) {
        logger().error(t, "Failed to pong. {}", msg);
    }
}
