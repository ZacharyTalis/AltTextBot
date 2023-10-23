package com.zacharytalis.alttextbot.services;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Queues;
import com.zacharytalis.alttextbot.board.v2.models.AltTextContribution;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

import java.awt.*;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

public class LeaderboardService {
    private static final Color EMBED_COLOR = Color.decode("#76737d");
    private static final ImmutableList<String> TOP_PLACE_PREFIXES =
        ImmutableList.of(":trophy:", ":second_place:", ":third_place:", ":four_leaf_clover:", ":star:");
    private static final String FALLBACK_PLACE_PREFIX = ":clap:";

    private final Server server;

    public LeaderboardService(Server server) {
        this.server = server;
    }

    public Multimap<Integer, AltTextContribution> getLeaderboard(int topN) {
        final var leaderboard = emptyLeaderboardMultiMap();
        final var contributionService = new AltTextContributionService(this.server);

        final var contributions = contributionService.orderedContributions();

        for (var contrib : contributions) {
            leaderboard.put(contrib.score(), contrib);
            if (leaderboard.keySet().size() == topN)
                break;
        }

        return leaderboard;
    }

    public EmbedBuilder getLeaderboardEmbed(int topN) {
        final var leaderboard = getLeaderboard(topN);
        final var embed = new EmbedBuilder().setColor(EMBED_COLOR);

        if (leaderboard.isEmpty()) {
            embed.setTitle("Alt-text never submitted on this server!");
        } else {
            embed.setTitle("This server's top alt-texters...");
            final var placeSymbolQueue = Queues.newArrayDeque(TOP_PLACE_PREFIXES);

            leaderboard.keySet().forEach(score -> {
                final var contributions = leaderboard.get(score);
                final var placeSymbol = Optional.ofNullable(placeSymbolQueue.pollFirst()).orElse(FALLBACK_PLACE_PREFIX);
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
                    placeSymbol,
                    "%s ~ **%d %s**".formatted(userNames, score, altText)
                );
            });
        }

        return embed;
    }

    private Multimap<Integer, AltTextContribution> emptyLeaderboardMultiMap() {
        return MultimapBuilder
            // Sort descending order of score
            .<Integer>treeKeys(Comparator.reverseOrder())
            .linkedListValues()
            .build();
    }
}
