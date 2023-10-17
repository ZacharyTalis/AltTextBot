package com.zacharytalis.alttextbot.services;

import com.zacharytalis.alttextbot.board.v2.AltTextContribution;
import com.zacharytalis.alttextbot.db.ConnectionPool;
import com.zacharytalis.alttextbot.db.dao.AltTextContributionDao;
import com.zacharytalis.alttextbot.db.dao.ServerDao;
import com.zacharytalis.alttextbot.db.dao.UserDao;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.util.List;

public class AltTextContributionService {
    private final Server server;

    public AltTextContributionService(Server server) {
        this.server = server;
    }

    public int increment(User contributor) {
        return ConnectionPool.withHandle(handle -> {
            final var uDao = handle.attach(UserDao.class);
            final var sDao = handle.attach(ServerDao.class);
            final var atcDao = handle.attach(AltTextContributionDao.class);

            final var user = uDao.fetchOrCreate(contributor.getId());
            final var server = sDao.fetchOrCreate(this.server.getId());
            final var contrib = atcDao.fetchOrCreate(user, server);

            return atcDao.increment(contrib).score();
        });
    }

    public List<AltTextContribution> orderedContributions() {
        return ConnectionPool.withHandle(handle -> {
            final var sDao = handle.attach(ServerDao.class);
            final var atcDao = handle.attach(AltTextContributionDao.class);

            final var serverOpt = sDao.fetchByDiscordId(this.server.getId());

            return serverOpt.map(s -> atcDao.orderedScores(s.discordId())).orElseGet(List::of);
        });
    }
}
