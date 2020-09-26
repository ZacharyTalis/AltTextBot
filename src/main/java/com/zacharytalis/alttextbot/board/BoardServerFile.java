package com.zacharytalis.alttextbot.board;

import com.zacharytalis.alttextbot.utils.Toolbox;

import java.io.*;
import java.util.HashMap;

/**
 * The class that contains server leaderboard info, and is serialized as such.
 */
public class BoardServerFile implements Serializable {

    private final Long serverID;
    private HashMap<Long, Integer> scoreMap = new HashMap<>();

    public BoardServerFile(Long serverID) {
        this.serverID = serverID;
        readAnyExistingFile();
        serializeThis();
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

    private void readAnyExistingFile() {
        try {
            if (new File(BoardUtils.generateDBPath(serverID)).exists()) {
                FileInputStream fileInputStream = new FileInputStream(BoardUtils.generateDBPath(serverID));
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                BoardServerFile readObject = (BoardServerFile) objectInputStream.readObject();
                fileInputStream.close();  objectInputStream.close();
                setScoreMap(readObject.getScoreMap());
            }
        } catch (FileNotFoundException fileNotFoundException) {
            Toolbox.getLogger("BoardServerFile replaceExistingFile").info("fileNotFoundException");
        } catch (IOException ioException) {
            Toolbox.getLogger("BoardServerFile replaceExistingFile").info("IOException");
        } catch (ClassNotFoundException classNotFoundException) {
            Toolbox.getLogger("BoardServerFile replaceExistingFile").info("ClassNotFoundException");
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
        } catch (FileNotFoundException fileNotFoundException) {
            Toolbox.getLogger("BoardServerFile serialize").info("fileNotFoundException");
        } catch (IOException ioException) {
            Toolbox.getLogger("BoardServerFile serialize").info("IOException");
        }
    }

}
