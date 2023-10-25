package com.zacharytalis.alttextbot.bangCommands.impl;

import com.zacharytalis.alttextbot.bangCommands.BaseCommandBody;
import com.zacharytalis.alttextbot.bangCommands.CommandInfo;
import com.zacharytalis.alttextbot.bangCommands.CommandMessage;
import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.utils.Ref;
import com.zacharytalis.alttextbot.utils.functions.Functions;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.user.User;

public class HelpCommand extends BaseCommandBody {
    public static CommandInfo description() {
        return new CommandInfo(
            "atbhelp",
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
    protected void receive(CommandMessage msg) {
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

        for (var cmd : bot().commands().values()) {
            mb.append(asHelpLine(cmd));
            mb.appendNewLine();
        }

        mb.append("**------------**");
        mb.appendNewLine();
        mb.append("We also now support slash commands! Try `/alt` instead of `!alt [alt-text]`.");

        return mb;
    }

    private String asHelpLine(final CommandInfo cmd) {
        return "%s ~ %s".formatted(cmd.bangName(), cmd.helpInfo());
    }

    private void handleSendFailed(final User user, final Throwable t) {
        logger().error(t, "Failed to send help text to user. {}", user.getDiscriminatedName());
    }
}
