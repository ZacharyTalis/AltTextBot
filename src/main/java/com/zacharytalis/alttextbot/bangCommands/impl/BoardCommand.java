package com.zacharytalis.alttextbot.bangCommands.impl;

import com.zacharytalis.alttextbot.bangCommands.BaseCommandBody;
import com.zacharytalis.alttextbot.bangCommands.CommandInfo;
import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.services.LeaderboardService;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.functions.Functions;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

public class BoardCommand extends BaseCommandBody {
    private static final int TOP_N = 5;

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
                msg.getChannel().sendMessage(makeEmbed(server))
                    .exceptionally(Functions.partialConsumer(this::handleSendFailure, msg));
            }
        );
    }

    private EmbedBuilder makeEmbed(final Server server) {
        final var service = new LeaderboardService(server);

        return service.getLeaderboardEmbed(TOP_N);
    }

    private void handleSendFailure(CommandMessage recv, Throwable t) {
        logger().error(t, "Failed to send leaderboard. {}", recv);
    }
}