package com.zacharytalis.alttextbot.utils;

import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.PermissionsBuilder;

public class Ref {
    public enum EnvType {
        TESTING,
        PRODUCTION;

        public static EnvType get(String name) {
            return valueOf(name.toUpperCase());
        }

        public String toString() {
            return switch (this) {
                case TESTING -> "Testing";
                case PRODUCTION -> "Production";
            };
        }
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
}
