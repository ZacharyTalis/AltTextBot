package com.zacharytalis.alttextbot.utils;

import com.zacharytalis.alttextbot.exceptions.InvalidEnvironmentError;
import com.zacharytalis.alttextbot.utils.config.IConfig;
import com.zacharytalis.alttextbot.utils.config.ProductionConfig;
import com.zacharytalis.alttextbot.utils.config.TestConfig;

public class Configs {
    public static IConfig getConfig(Ref.EnvType envType) {
        return switch (envType) {
            case TESTING -> new TestConfig();
            case PRODUCTION -> new ProductionConfig();
        };
    }

    public static IConfig getConfigFromEnv() throws InvalidEnvironmentError, IllegalArgumentException {
        final var envType = System.getenv(Ref.ENV_MODE_VAR);

        if (envType == null)
            throw new InvalidEnvironmentError("Missing %s environment variable", Ref.ENV_MODE_VAR);

        try {
            return getConfig(Ref.EnvType.get(envType));
        } catch (IllegalArgumentException e) {
            throw new InvalidEnvironmentError(e, "%s is not a valid environment", envType);
        }
    }
}
