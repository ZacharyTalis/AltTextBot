package com.zacharytalis.alttextbot.bangCommands.impl;

import com.zacharytalis.alttextbot.bangCommands.BaseCommandBody;
import com.zacharytalis.alttextbot.bangCommands.CommandInfo;
import com.zacharytalis.alttextbot.bangCommands.CommandMessage;
import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.commands.runners.BoardRunner;
import com.zacharytalis.alttextbot.commands.runners.IBoardProvider;
import com.zacharytalis.alttextbot.utils.functions.Functions;
import org.javacord.api.entity.server.Server;

public class BoardCommand extends BaseCommandBody {
    private record Provider(Server server) implements IBoardProvider {
    }

    public static CommandInfo description() {
        return new CommandInfo(
            "atbboard",
            "Display the current server's top alt-texters.",
            BoardCommand::new
        );
    }

    public BoardCommand(AltTextBot bot) {
        super(bot);
    }

    @Override
    public CommandInfo getInfo() {
        return BoardCommand.description();
    }

    @Override
    protected void receive(CommandMessage msg) {
        msg.getServer().ifPresent(
            server -> {
                final var runner = new BoardRunner(new Provider(server));

                msg.getChannel().sendMessage(runner.getLeaderboardEmbed())
                    .exceptionally(Functions.partialConsumer(this::handleSendFailure, msg));
            }
        );
    }

    private void handleSendFailure(CommandMessage recv, Throwable t) {
        logger().error(t, "Failed to send leaderboard. {}", recv);
    }
}
