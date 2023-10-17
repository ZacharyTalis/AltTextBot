package com.zacharytalis.alttextbot.utils.config;

import com.zacharytalis.alttextbot.utils.Ref;

import java.util.Map;
import java.util.Optional;

public abstract class ConfigBase implements Config {
    private final Map<String, String> env = System.getenv();

    final Map<String, String> env() {
        return env;
    }

    Optional<String> fetch(String name) {
        if (env.containsKey(name))
            return Optional.of(env.get(name));
        else
            return Optional.empty();
    }

    String require(String name) throws ConfigurationException {
        return fetch(name).orElseThrow(() -> new ConfigurationException("{} not found in environment", name));
    }

    @Override
    public String getDbPath() throws ConfigurationException {
        return require(Ref.DB_PATH_VAR);
    }

    @Override
    public String getDbUrl() throws ConfigurationException {
        return require(Ref.DB_URL_VAR);
    }
}
