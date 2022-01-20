package com.zacharytalis.alttextbot.commands.impl;

import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.commands.BaseCommandBody;
import com.zacharytalis.alttextbot.commands.CommandInfo;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.Ref;
import com.zacharytalis.alttextbot.utils.functions.Functions;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.user.User;

public class HelpCommand extends BaseCommandBody {
    public static CommandInfo description() {
        return new CommandInfo(
            "!atbhelp",
            "Get all commands from AltTextBot in a direct message.",
            HelpCommand::new
        );
    }

    public HelpCommand(AltTextBot bot) {
        super(bot);
    }

    @Override
    public CommandInfo getInfo() {
        return HelpCommand.description();
    }

    @Override
    protected void call(CommandMessage msg) {
        msg.getUserAuthor().ifPresent(this::sendHelpText);
    }

    private void sendHelpText(final User user) {
        getHelpMessage()
            .send(user)
            .exceptionally(Functions.partialConsumer(this::handleSendFailed, user));
    }

    private MessageBuilder getHelpMessage() {
        final var header = """
        Commands available for **%s v%s**:
        **------------**
        """.formatted(bot().internalName(), Ref.BOT_VERSION);

        final var mb = new MessageBuilder().append(header);

        for(var cmd : bot().commands().values()) {
            mb.append(asHelpLine(cmd));
        }

        return mb;
    }

    private String asHelpLine(final CommandInfo cmd) {
        return "%s ~ %s".formatted(cmd.name(), cmd.helpInfo());
    }

    private void handleSendFailed(final User user, final Throwable t) {
        logger().error(t, "Failed to send help text to user. {}", user.getDiscriminatedName());
    }
}
