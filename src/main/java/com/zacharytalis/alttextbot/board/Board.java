package com.zacharytalis.alttextbot.board;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Created by BoardUtils each time a leaderboard is accessed.
 * This class gets passed to BoardCommand via BoardUtils.
 */
@Deprecated
public class Board {

    BoardServerFile boardServerFile;
    EmbedBuilder embedBuilder;
    static final int MAXPLACES = 5;

    public Board(BoardServerFile boardServerFile, Server server) {
        this.boardServerFile = boardServerFile;
        embedBuilder = makeEmbedBuilder(server);
    }

    public EmbedBuilder getEmbedBuilder() {
        return embedBuilder;
    }

    private EmbedBuilder makeEmbedBuilder(Server server) {

        // Find the top-five placingUsers

        List<Map.Entry<Long, Integer>> scoreList = BoardUtils.getSortedScores(boardServerFile.getScoreMap());
        int scoreListIndex = 0;
        ArrayList<Integer> scorePrintList = new ArrayList<>(MAXPLACES);

        AtomicReference<ArrayList<List<String>>> userList = new AtomicReference<>
                (new ArrayList<>(MAXPLACES));

        for (int i = 0; i < MAXPLACES; i++) {
            userList.get().add(new LinkedList<>());
        }

        int userListIndex = 0;

        final String embedColor = "#76737d";

        if (scoreList.size() <= 0) {
            return new EmbedBuilder()
                    .setTitle("Alt-text never submitted on this server!")
                    .setColor(Color.decode(embedColor));
        }

        Integer whichScore = scoreList.get(0).getValue();
        scorePrintList.add(scoreListIndex, whichScore);

        //     check if userList is full and if scoreList has entries remaining
        while (userListIndex <= MAXPLACES-1 && scoreListIndex < scoreList.size()) {

            // add a user to the current List in userList
            if (scoreList.get(scoreListIndex).getValue().equals(whichScore)) {
                userList.get().get(userListIndex)
                        .add(BoardUtils.getDisplayNameFromID(server, scoreList.get(scoreListIndex).getKey()));
            }

            // add a user to the next List in userList, and thus add a score to scorePrintList
            else if (userListIndex < MAXPLACES-1) {
                userListIndex++;
                userList.get().get(userListIndex)
                        .add(BoardUtils.getDisplayNameFromID(server, scoreList.get(scoreListIndex).getKey()));
                whichScore = scoreList.get(scoreListIndex).getValue();
                scorePrintList.add(userListIndex, whichScore);
            }

            // adding another user to userList would exceed capacity, so end while loop
            else userListIndex++;

            // use the next entry in scoreList
            scoreListIndex++;

        }

        final ArrayList<String> userPrintList =
                userList.get().stream().map(BoardUtils::userListString)
                        .collect(Collectors.toCollection(() -> new ArrayList<>(MAXPLACES)));
        final String[] fieldNames = new String[]
                {":trophy:", ":second_place:", ":third_place:", ":four_leaf_clover:", ":star:"};

        EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("This server's top alt-texters...");
        int i = 0;
        while (i < MAXPLACES && !userPrintList.get(i).equals("")) {

            // useful when a placing user has submitted only a single alt-text
            String altString;
            if (scorePrintList.get(i) == 1) altString = " alt-text**";
            else altString = " alt-texts**";

            embedBuilder.addField(fieldNames[i],
                    userPrintList.get(i) + " ~ **" + scorePrintList.get(i) + altString, false);
            i++;

        } return embedBuilder.setColor(Color.decode(embedColor));
    }
}
