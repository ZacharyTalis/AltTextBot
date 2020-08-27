package com.zacharytalis.alttextbot.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;

/**
 * Contains saved info for functions to pull from.
 */
public class Info implements Serializable {

    private final String INFOPATH = "data/info.log";
    private String today;
    private LinkedList<String> datesPlayed = new LinkedList<>();

    public Info(String today) {
        this.today = today;
        if (!datesPlayed.contains(today)) datesPlayed.add(today);
    }

    public String getToday() { return today; }
    public void setToday(String lastDate) {
        this.today = lastDate;
        if (!datesPlayed.contains(today)) datesPlayed.add(today);
        overwrite();
    }

    public boolean hasTodayBeenPosted(String today) {
        return datesPlayed.contains(today);
    }

    private void overwrite() {

        try {
            ObjectOutputStream writesOne = new ObjectOutputStream(new FileOutputStream(INFOPATH));
            writesOne.writeObject(this);
        } catch (IOException exc) {
            System.out.println("times.log not found, or has been corrupted.");
        }

    }

}
