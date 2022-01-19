package com.zacharytalis.alttextbot.utils.config;

import com.zacharytalis.alttextbot.utils.Ref;

public interface Config {
    String getToken() throws ConfigurationException;

    String getDbPath() throws ConfigurationException;

    String getDbUrl() throws ConfigurationException;

    Ref.EnvType getEnv();
}
