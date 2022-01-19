package com.zacharytalis.alttextbot.commands.impl;

import com.google.common.collect.*;
import com.zacharytalis.alttextbot.board.Board;
import com.zacharytalis.alttextbot.board.BoardUtils;
import com.zacharytalis.alttextbot.board.v2.AltTextContribution;
import com.zacharytalis.alttextbot.board.v2.User;
import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.commands.BaseCommandBody;
import com.zacharytalis.alttextbot.commands.CommandInfo;
import com.zacharytalis.alttextbot.db.ConnectionPool;
import com.zacharytalis.alttextbot.db.dao.AltTextContributionDao;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.DiscordEntities;
import com.zacharytalis.alttextbot.utils.functions.Functions;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

import java.awt.*;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class BoardCommand extends BaseCommandBody {
    private static final int TOP_N = 5;
    private static final Color EMBED_COLOR = Color.decode("#76737d");
    private static final ImmutableList<String> TOP_PLACE_PREFIXES =
            ImmutableList.of(":trophy:", ":second_place:", ":third_place:", ":four_leaf_clover:", ":star:");
    private static final String FALLBACK_PLACE_PREFIX = ":clap:";

    public static CommandInfo description() {
        return new CommandInfo(
            "!atbboard",
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
    protected void call(CommandMessage msg) {
        msg.getServer().ifPresent(
            server -> {
                final var scoresInServer =
                    ConnectionPool.withExtension(
                            AltTextContributionDao.class,
                            atcd -> atcd.serverScores(msg.getServerID())
                    );

                msg.getChannel().sendMessage(makeEmbed(server, scoresInServer))
                        .exceptionally(Functions.partialConsumer(this::handleSendFailure, msg));
            }
        );
    }

    private EmbedBuilder makeEmbed(final Server server, final List<AltTextContribution> leaderboard) {
        final var embed = new EmbedBuilder().setColor(EMBED_COLOR);

        if (leaderboard.isEmpty()) {
            embed.setTitle("Alt-text never submitted on this server!");
        } else {
            final ListMultimap<Integer, AltTextContribution> contribsByScore =
                    MultimapBuilder.<Integer>treeKeys(Comparator.reverseOrder()).linkedListValues().build();

            leaderboard.sort(Comparator.reverseOrder());

            for(var contrib : leaderboard) {
                contribsByScore.put(contrib.score(), contrib);

                if (contribsByScore.keySet().size() >= TOP_N)
                    break;
            }

            final var placePrefixQueue = Queues.newArrayDeque(TOP_PLACE_PREFIXES);

            embed.setTitle("This server's top alt-texters...");
            contribsByScore.keySet().forEach(score -> {
                final var contributions = contribsByScore.get(score);
                final var placePrefix = Optional.ofNullable(placePrefixQueue.pollFirst()).orElse(FALLBACK_PLACE_PREFIX);
                final var altText = score == 1 ? "alt-text" : "alt-texts";

                final var userNames =
                    contributions.stream()
                        .map(AltTextContribution::getUser)
                        .map(user ->
                            server
                                .getMemberById(user.discordId())
                                .map(server::getDisplayName)
                                .orElse("*Member left*")
                        )
                        .collect(Collectors.joining(", "));

                embed.addField(
                    placePrefix,
                    "%s ~ **%d %s**".formatted(userNames, score, altText)
                );
            });
        }

        return embed;
    }

    private void handleSendFailure(CommandMessage recv, Throwable t) {
        logger().error(t, "Failed to send leaderboard. {}", recv);
    }
}
