package com.zacharytalis.alttextbot.board;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import java.awt.*;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Created by BoardManager each time a leaderboard is accessed.
 * This class gets passed to BoardCommand via BoardManager.
 */
public class Board {

    ArrayList<String> placingUsers;
    BoardServerFile boardServerFile;

    public Board(BoardServerFile boardServerFile) {

        this.boardServerFile = boardServerFile;

        // Generate the contents of Board using the BoardServerFile.

    }

    public EmbedBuilder getEmbedBuilder(Optional<Server> server) {

        // Find the top-five placingUsers

        return new EmbedBuilder()
                .setTitle("This server's top alt-texters...")
                .addField(":trophy:", "User1 ~ X alt-texts", false)
                .addField(":second_place:", "User2 ~ X alt-texts", false)
                .addField(":third_place:", "User3 ~ X alt-texts", false)
                .addField(":four_leaf_clover:", "User4 ~ X alt-texts", false)
                .addField(":star:", "User5 ~ X alt-texts", false)
                .setColor(Color.decode("#76737d"));
    }
}
