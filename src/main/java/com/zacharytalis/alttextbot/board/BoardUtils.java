package com.zacharytalis.alttextbot.board;

import com.zacharytalis.alttextbot.exceptions.InvalidEnvironmentException;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.Configs;
import com.zacharytalis.alttextbot.utils.config.ConfigurationException;
import org.javacord.api.entity.server.Server;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilities for managing BoardServerFiles, and sending the included file info to BoardCommand as new Boards.
 */
public class BoardUtils {

    public static Board newBoardByMsg(CommandMessage msg) {
        return new Board(new BoardServerFile(msg.getServerID()), msg.getServer().orElse(null));
    }

    public static String generateDBPath(Long serverID) {
        try {
            final var config = Configs.getConfigFromEnv();
            return Paths.get(config.getDbPath(), serverID + ".db").toString();
        } catch (ConfigurationException | InvalidEnvironmentException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Map.Entry<Long, Integer>> getSortedScores(HashMap<Long, Integer> scoreMap) {
        final Comparator<Map.Entry<Long, Integer>> compareByValueDesc =
                Collections.reverseOrder(Map.Entry.comparingByValue());
        return scoreMap.entrySet().stream().sorted(compareByValueDesc).collect(Collectors.toList());
    }

    public static String userListString(List<String> usersList) {
        String usersString = "";  int i = 0;
        for (String user : usersList) {
            i++;
            usersString = usersString.concat(user);
            if (i < usersList.size()) usersString = usersString.concat(", ");
        } return usersString;
    }

    public static String getDisplayNameFromID(Server server, Long userID) {
        return server.getDisplayName(server.getMemberById(userID).orElse(null));
    }

}
