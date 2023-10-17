package com.zacharytalis.alttextbot.db.dao;

import com.zacharytalis.alttextbot.board.v2.Server;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

@RegisterConstructorMapper(Server.class)
public interface ServerDao {
    @SqlQuery("SELECT * FROM servers WHERE id = ?;")
    Optional<Server> find(@Bind int id);

    @SqlUpdate("""
            INSERT INTO servers (discord_id, created_at, updated_at)
                VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);            
        """)
    @GetGeneratedKeys
    Server create(@Bind long discordId);

    @SqlQuery("SELECT * FROM servers WHERE discord_id = ?;")
    Optional<Server> fetchByDiscordId(@Bind long discordId);

    default Server fetchOrCreate(long discordId) {
        return fetchByDiscordId(discordId).orElseGet(() -> create(discordId));
    }
}
