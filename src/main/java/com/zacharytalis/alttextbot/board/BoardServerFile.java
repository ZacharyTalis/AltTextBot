package com.zacharytalis.alttextbot.board;

import com.zacharytalis.alttextbot.board.v2.AltTextContribution;
import com.zacharytalis.alttextbot.db.ConnectionPool;
import com.zacharytalis.alttextbot.db.dao.AltTextContributionDao;
import com.zacharytalis.alttextbot.db.dao.ServerDao;
import com.zacharytalis.alttextbot.db.dao.UserDao;
import com.zacharytalis.alttextbot.utils.Toolbox;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The class that contains server leaderboard info, and is serialized as such.
 */
@Deprecated
public class BoardServerFile implements Serializable {
    private static final long serialVersionUID = -4769697229062818220L;

    public final Long serverID;
    private HashMap<Long, Integer> scoreMap = new HashMap<>();

    public BoardServerFile(Long serverID) {
        this.serverID = serverID;
        readAnyExistingFile();
        serializeThis();
        Toolbox.inferLogger().info("{} entries for {}", scoreMap.size(), serverID);
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public BoardServerFile(Long serverID, Long userID) {
        this.serverID = serverID;
        readAnyExistingFile();
        incrementUserScore(userID);
        serializeThis();
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public BoardServerFile(Long serverID, Long userID, Integer userScore) {
        this.serverID = serverID;
        readAnyExistingFile();
        setUserScore(userID, userScore);
        serializeThis();
    }

    public List<AltTextContribution> migrateToDatabase() {
        final long serverId = this.serverID;
        return scoreMap.keySet().stream().mapToLong(Long::longValue).mapToObj(userId -> {
            final var score = scoreMap.get(userId);

            final var user = ConnectionPool.withExtension(UserDao.class, ud -> ud.fetchOrCreate(userId));
            final var server = ConnectionPool.withExtension(ServerDao.class, sd -> sd.fetchOrCreate(serverId));
            return ConnectionPool.withExtension(AltTextContributionDao.class, atcd -> {
                // Only new scores are accounted for
                final var contrib = atcd.fetchOrCreate(user, server);
                return contrib.score() == 0 ? atcd.increaseBy(contrib, score) : null;
            });
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void readAnyExistingFile() {
        try {
            if (new File(BoardUtils.generateDBPath(serverID)).exists()) {
                FileInputStream fileInputStream = new FileInputStream(BoardUtils.generateDBPath(serverID));
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                BoardServerFile readObject = (BoardServerFile) objectInputStream.readObject();
                fileInputStream.close();  objectInputStream.close();
                setScoreMap(readObject.getScoreMap());
            }
        } catch (Exception fileNotFoundException) {
            Toolbox.uncheckedThrow(fileNotFoundException);
        }
    }

    public HashMap<Long, Integer> getScoreMap() {
        return scoreMap;
    }

    public void setScoreMap(HashMap<Long, Integer> scoreMap) {
        this.scoreMap = scoreMap;
    }

    public void incrementUserScore(Long userID) {
        if (scoreMap.containsKey(userID)) scoreMap.put(userID, scoreMap.get(userID)+1);
        else scoreMap.put(userID, 1);
    }

    public void setUserScore(Long userID, Integer userScore) {
        scoreMap.put(userID, userScore);
    }

    public void serializeThis() {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(BoardUtils.generateDBPath(serverID));
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(this);
            fileOutputStream.close();  objectOutputStream.close();
        } catch (Throwable fileNotFoundException) {
            Toolbox.uncheckedThrow(fileNotFoundException);
        }
    }

}
