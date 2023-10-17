package com.zacharytalis.alttextbot.bots;

import com.zacharytalis.alttextbot.bangCommands.registry.CommandRegistry;
import com.zacharytalis.alttextbot.bangCommands.registry.ICommandRegistry;

public interface DiscordBotInfo {
    String internalName();

    String version();

    ICommandRegistry<CommandRegistry> commands();
}
