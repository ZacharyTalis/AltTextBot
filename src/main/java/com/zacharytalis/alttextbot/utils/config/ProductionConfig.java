package com.zacharytalis.alttextbot.utils.config;

import com.zacharytalis.alttextbot.utils.Ref;

public class ProductionConfig extends ConfigBase {
    @Override
    public String getToken() throws ConfigurationException {
        return require(Ref.TOKEN_VAR);
    }

    @Override
    public Ref.EnvType getEnv() {
        return Ref.EnvType.PRODUCTION;
    }
}
