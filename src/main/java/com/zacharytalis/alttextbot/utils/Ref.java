package com.zacharytalis.alttextbot.utils;

import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.PermissionsBuilder;

public class Ref {
    public enum EnvType {
        TESTING {
            @Override
            public String toString() {
                return "Testing";
            }

            @Override
            public boolean isTesting() {
                return true;
            }

            @Override
            public boolean isProduction() {
                return false;
            }
        },
        PRODUCTION {
            @Override
            public String toString() {
                return "Production";
            }

            @Override
            public boolean isTesting() {
                return false;
            }

            @Override
            public boolean isProduction() {
                return true;
            }
        };

        public static EnvType get(String name) {
            return valueOf(name.toUpperCase());
        }

        public abstract boolean isTesting();
        public abstract boolean isProduction();
    }

    public static final String TOKEN_VAR = "BOT_TOKEN";
    public static final String TEST_TOKEN_VAR = "TEST_BOT_TOKEN";
    public static final String ENV_MODE_VAR = "BOT_ENV";

    public static final Permissions REQUIRED_PERMS =
        new PermissionsBuilder()
            .setAllDenied()
            .setAllowed(
                PermissionType.MANAGE_MESSAGES,
                PermissionType.READ_MESSAGES,
                PermissionType.SEND_MESSAGES
            ).build();

    public static EnvType currentEnv() {
        return EnvType.get(System.getenv(ENV_MODE_VAR));
    }
}
