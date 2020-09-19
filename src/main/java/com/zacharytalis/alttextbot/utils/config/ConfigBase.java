package com.zacharytalis.alttextbot.utils.config;

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
}
