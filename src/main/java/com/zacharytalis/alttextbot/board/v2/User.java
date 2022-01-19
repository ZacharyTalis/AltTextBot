package com.zacharytalis.alttextbot.board.v2;

import java.time.ZonedDateTime;

public record User(int id, long discordId, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
}
