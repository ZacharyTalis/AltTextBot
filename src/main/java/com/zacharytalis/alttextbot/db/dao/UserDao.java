package com.zacharytalis.alttextbot.db.dao;

import com.zacharytalis.alttextbot.board.v2.User;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

@RegisterConstructorMapper(User.class)
public interface UserDao {
    @SqlQuery("SELECT * FROM users WHERE id = ?;")
    Optional<User> find(@Bind int id);

    @SqlUpdate("""
            INSERT INTO users (discord_id, created_at, updated_at)
                VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);            
        """)
    @GetGeneratedKeys
    User create(@Bind long discordId);

    @SqlQuery("SELECT * FROM users WHERE discord_id = ?;")
    Optional<User> fetchByDiscordId(@Bind long discordId);

    default User fetchOrCreate(long discordId) {
        return fetchByDiscordId(discordId).orElseGet(() -> create(discordId));
    }
}
