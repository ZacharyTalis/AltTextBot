package com.zacharytalis.alttextbot.db.dao;

import com.zacharytalis.alttextbot.board.v2.AltTextContribution;
import com.zacharytalis.alttextbot.board.v2.Server;
import com.zacharytalis.alttextbot.board.v2.User;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

@RegisterConstructorMapper(AltTextContribution.class)
public interface AltTextContributionDao {
    @SqlQuery("SELECT * FROM alt_text_contributions WHERE id = ?")
    Optional<AltTextContribution> find(@Bind int id);

    @SqlQuery("""
        SELECT
            atc.*
        FROM alt_text_contributions atc
            JOIN servers s on atc.server_id = s.id
            JOIN users u ON atc.user_id = u.id
        WHERE s.discord_id = :discordId
    """)
    List<AltTextContribution> serverScores(@Bind("discordId") long discordId);

    @SqlUpdate("""
        INSERT INTO alt_text_contributions (server_id, user_id, score, created_at, updated_at, last_contribution_at)
            VALUES (:server.id, :user.id, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
        ON CONFLICT (server_id, user_id) DO UPDATE SET server_id = excluded.server_id
    """)
    @GetGeneratedKeys
    AltTextContribution fetchOrCreate(@BindMethods("user") User user, @BindMethods("server") Server server);

    @SqlUpdate("""
        UPDATE alt_text_contributions atc
           SET score = score + 1,
               updated_at = CURRENT_TIMESTAMP,
               last_contribution_at = CURRENT_TIMESTAMP
        WHERE atc.id = :contrib.id
    """)
    @GetGeneratedKeys
    AltTextContribution increment(@BindMethods("contrib") AltTextContribution atc);

    @SqlUpdate("""
      UPDATE alt_text_contributions atc
         SET score = score + :summand,
             updated_at = CURRENT_TIMESTAMP,
             last_contribution_at = CURRENT_TIMESTAMP
      WHERE atc.id = :contrib.id
    """)
    @GetGeneratedKeys
    AltTextContribution increaseBy(@BindMethods("contrib") AltTextContribution atc, @Bind("summand") int delta);
}
