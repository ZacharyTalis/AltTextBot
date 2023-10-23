package com.zacharytalis.alttextbot.board.v2.models;

import java.time.ZonedDateTime;

public record User(int id, long discordId, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
}
