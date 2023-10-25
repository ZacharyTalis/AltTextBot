package com.zacharytalis.alttextbot.commands.runners;

import com.zacharytalis.alttextbot.services.LeaderboardService;
import org.javacord.api.entity.message.embed.EmbedBuilder;

public class BoardRunner {
    private static final int TOP_N = 5;

    private IBoardProvider provider;

    public BoardRunner(IBoardProvider provider) {
        this.provider = provider;
    }

    public EmbedBuilder getLeaderboardEmbed() {
        final var leaderboardService = new LeaderboardService(provider.server());

        return leaderboardService.getLeaderboardEmbed(TOP_N);
    }
}
