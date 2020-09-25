package com.zacharytalis.alttextbot.board;

import com.zacharytalis.alttextbot.exceptions.InvalidEnvironmentException;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.Configs;
import com.zacharytalis.alttextbot.utils.config.ConfigurationException;

import java.nio.file.Paths;

/**
 * Used for managing BoardServerFiles, and sending the included file info to BoardCommand as new Boards.
 */
public class BoardManager {

    public static Board newBoardByMsg(CommandMessage msg) {
        return new Board(new BoardServerFile(msg.getServerID()));
    }

    public static String generateDBPath(Long serverID) {
        try {
            final var config = Configs.getConfigFromEnv();
            return Paths.get(config.getDbPath(), serverID + ".db").toString();
        } catch (ConfigurationException | InvalidEnvironmentException e) {
            throw new RuntimeException(e);
        }
    }

}
