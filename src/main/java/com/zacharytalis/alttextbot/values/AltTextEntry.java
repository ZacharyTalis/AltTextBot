package com.zacharytalis.alttextbot.values;

import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

public record AltTextEntry(User user, Server server, String altText) {
}
