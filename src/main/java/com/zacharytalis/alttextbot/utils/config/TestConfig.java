package com.zacharytalis.alttextbot.utils.config;

import com.zacharytalis.alttextbot.utils.Ref;

public class TestConfig extends ConfigBase {
    @Override
    public String getToken() throws ConfigurationException {
        return require(Ref.TEST_TOKEN_VAR);
    }

    @Override
    public Ref.EnvType getEnv() {
        return Ref.EnvType.TESTING;
    }
}
