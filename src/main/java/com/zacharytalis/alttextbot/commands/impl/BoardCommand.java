package com.zacharytalis.alttextbot.commands.impl;

import com.zacharytalis.alttextbot.board.Board;
import com.zacharytalis.alttextbot.board.BoardManager;
import com.zacharytalis.alttextbot.bots.AltTextBot;
import com.zacharytalis.alttextbot.commands.BaseCommandBody;
import com.zacharytalis.alttextbot.commands.CommandInfo;
import com.zacharytalis.alttextbot.utils.CommandMessage;

public class BoardCommand extends BaseCommandBody {

    public static CommandInfo description() {
        return new CommandInfo(
            "!atbboard",
            "Post the AltTextBot leaderboard for this server.",
            BoardCommand::new
        );
    }

    public BoardCommand(AltTextBot bot) {
        super(bot);
    }

    @Override
    public CommandInfo getInfo() {
        return BoardCommand.description();
    }

    @Override
    protected void call(CommandMessage msg) {

        // Get the new leaderboard info to display.
        Board board = BoardManager.newBoardByMsg(msg);

        // Send the embed message.
        msg.getChannel().sendMessage(board.getEmbedBuilder(msg.getServer()));
    }
}

