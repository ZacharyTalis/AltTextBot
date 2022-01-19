package com.zacharytalis.alttextbot.db;

import com.zacharytalis.alttextbot.utils.Configs;
import com.zacharytalis.alttextbot.utils.Toolbox;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.util.function.Consumer;
import java.util.function.Function;

public class ConnectionPool {
    private static final HikariDataSource hikariDataSource = new HikariDataSource();

    static {
        var cfg = Toolbox.unchecked(Configs::getConfigFromEnv).get();
        var url = Toolbox.unchecked(cfg::getDbUrl).get();

        hikariDataSource.setJdbcUrl(url);
    }

    private static final Jdbi jdbi = Jdbi.create(hikariDataSource);

    static {
        jdbi.installPlugins();
    }

    public static MigrateResult migrate() {
        final var flyway = Flyway.configure().dataSource(hikariDataSource).load();
        return flyway.migrate();
    }

    public static Jdbi getJdbi() {
        return ConnectionPool.jdbi;
    }

    public static <R, T> R withExtension(Class<T> extensionClass, Function<T, R> fn) {
        return getJdbi().withExtension(extensionClass, fn::apply);
    }

    public static <R> R withHandle(Function<Handle, R> applied) {
        return getJdbi().withHandle(applied::apply);
    }

    public static void useHandle(Consumer<Handle> consumer) {
        getJdbi().useHandle(consumer::accept);
    }
}
