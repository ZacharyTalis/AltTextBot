package com.zacharytalis.alttextbot.board.v2;

import com.zacharytalis.alttextbot.db.ConnectionPool;
import com.zacharytalis.alttextbot.db.dao.UserDao;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;

public record AltTextContribution(
    int id,
    int userId,
    int serverId,
    int score,
    ZonedDateTime createdAt,
    ZonedDateTime updatedAt,
    ZonedDateTime lastContributionAt)
    implements Comparable<AltTextContribution> {

    public User getUser() {
        return ConnectionPool.withExtension(UserDao.class, ud -> ud.find(AltTextContribution.this.userId()).orElseThrow());
    }

    @Override
    public int compareTo(@NotNull AltTextContribution o) {
        return Integer.compareUnsigned(score(), o.score());
    }
}
