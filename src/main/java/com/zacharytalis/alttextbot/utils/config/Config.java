package com.zacharytalis.alttextbot.utils.config;

import com.zacharytalis.alttextbot.utils.Ref;

public interface Config {
    String getToken() throws ConfigurationException;

    Ref.EnvType getEnv();
}
