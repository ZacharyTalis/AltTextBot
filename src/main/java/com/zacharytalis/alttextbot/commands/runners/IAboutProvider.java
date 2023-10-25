package com.zacharytalis.alttextbot.commands.runners;

import com.zacharytalis.alttextbot.bots.DiscordBot;
import org.javacord.api.DiscordApi;

public interface IAboutProvider {
    DiscordApi api();

    DiscordBot<?> bot();
}
