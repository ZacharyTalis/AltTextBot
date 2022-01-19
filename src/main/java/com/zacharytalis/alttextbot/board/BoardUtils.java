package com.zacharytalis.alttextbot.board;

import com.zacharytalis.alttextbot.exceptions.InvalidEnvironmentException;
import com.zacharytalis.alttextbot.utils.CommandMessage;
import com.zacharytalis.alttextbot.utils.Configs;
import com.zacharytalis.alttextbot.utils.Toolbox;
import com.zacharytalis.alttextbot.utils.config.ConfigurationException;
import org.javacord.api.entity.server.Server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities for managing BoardServerFiles, and sending the included file info to BoardCommand as new Boards.
 */
@Deprecated
public class BoardUtils {

    public static Board newBoardByMsg(CommandMessage msg) {
        return new Board(new BoardServerFile(msg.getServerID()), msg.getServer().orElse(null));
    }

    // Very temporary
    public static Stream<BoardServerFile> getAllBoardFiles() throws IOException {
        final var config = Toolbox.unchecked(Configs::getConfigFromEnv).get();
        final var dbPath = Toolbox.unchecked(config::getDbPath).get();

        final var paths = Files.list(Path.of(dbPath)).toList();
        paths.forEach(p -> Toolbox.inferLogger().info("Considering {} for FS -> DB migration", p.toString()));

        return Files.list(Path.of(dbPath)).filter(Files::isRegularFile).map(Path::getFileName).map(Path::toString).filter(s -> !s.startsWith(".")).map(com.google.common.io.Files::getNameWithoutExtension).map(Long::parseLong).map(BoardServerFile::new);
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
        return server.getMemberById(userID).map(server::getDisplayName).orElse("*Member left*");
    }

}
