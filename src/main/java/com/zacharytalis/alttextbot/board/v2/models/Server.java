package com.zacharytalis.alttextbot.board.v2.models;

import java.time.ZonedDateTime;

public record Server(int id, long discordId, ZonedDateTime createdAt, ZonedDateTime updatedAt) {

}
