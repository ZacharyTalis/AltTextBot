package com.zacharytalis.alttextbot.utils;

import com.zacharytalis.alttextbot.exceptions.InvalidEnvironmentException;
import com.zacharytalis.alttextbot.utils.config.Config;
import com.zacharytalis.alttextbot.utils.config.ProductionConfig;
import com.zacharytalis.alttextbot.utils.config.TestConfig;

public class Configs {
    public static Config getConfig(Ref.EnvType envType) {
        return switch (envType) {
            case TESTING -> new TestConfig();
            case PRODUCTION -> new ProductionConfig();
        };
    }

    public static Config getConfigFromEnv() throws InvalidEnvironmentException, IllegalArgumentException {
        final var envType = System.getenv(Ref.ENV_MODE_VAR);

        if (envType == null)
            throw new InvalidEnvironmentException("Missing %s environment variable", Ref.ENV_MODE_VAR);

        try {
            return getConfig(Ref.EnvType.get(envType));
        } catch (IllegalArgumentException e) {
            throw new InvalidEnvironmentException(e, "%s is not a valid environment", envType);
        }
    }
}
