package com.zacharytalis.alttextbot.utils;

import com.google.common.collect.ImmutableList;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.user.User;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Ref {
    public record ProjectAuthor(String name, Long discordId) {
        public record ProjectAuthorWithUser(String name, Long discordId, User user) {
        }

        public CompletableFuture<ProjectAuthorWithUser> withUser(DiscordApi api) {
            return api.getUserById(discordId).thenApply(user -> new ProjectAuthorWithUser(name, discordId, user));
        }
    }

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
    public static final String DB_PATH_VAR = "DB_PATH";
    public static final String DB_URL_VAR = "DB_URL";
    public static final String BOT_VERSION_VAR = "BOT_VERSION";

    public static final String GITHUB_REPO = "https://github.com/ZacharyTalis/AltTextBot";
    public static final List<ProjectAuthor> authors = ImmutableList.of(
        new ProjectAuthor("Zachary Talis", 133066971867643904L),
        new ProjectAuthor("autumn/glossawy", 802365639716765716L)
    );

    public static final String BOT_VERSION = System.getenv().getOrDefault(BOT_VERSION_VAR, "unkown (dev)");

    public static final Permissions REQUIRED_PERMS =
        new PermissionsBuilder()
            .setAllDenied()
            .setAllowed(
                PermissionType.MANAGE_MESSAGES,
                PermissionType.READ_MESSAGE_HISTORY,
                PermissionType.SEND_MESSAGES
            ).build();

    public static EnvType currentEnv() {
        return EnvType.get(System.getenv(ENV_MODE_VAR));
    }
}
