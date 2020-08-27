/*
 * Copyright (C) 2017 Bastian Oppermann
 * 
 * This file is part of Javacord.
 * 
 * Javacord is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser general Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Javacord is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package com.zacharytalis.alttextbot.exceptions;

import com.zacharytalis.alttextbot.entities.Channel;
import com.zacharytalis.alttextbot.entities.Server;
import com.zacharytalis.alttextbot.utils.ratelimits.RateLimitManager;
import com.zacharytalis.alttextbot.utils.ratelimits.RateLimitType;

/**
 * This exception is always thrown if we receive a response which has rate_limit.
 */
public class RateLimitedException extends Exception {

    private final long retryAfter;
    private final RateLimitType type;
    private final Server server;
    private final Channel channel;
    private final RateLimitManager manager;

    /**
     * Creates a new instance of this class.
     *
     * @param message The message of the exception.
     * @param retryAfter How long we should wait, till we can try again.
     * @param type The type of the rate limit.
     * @param server The server of the rate limit. Can be <code>null</code> for non-server related limits.
     * @param channel The channel of the rate limit. Can be <code>null</code> for non-channel related limits.
     * @param manager The rate limit manager.
     */
    public RateLimitedException(String message, long retryAfter, RateLimitType type, Server server, Channel channel, RateLimitManager manager) {
        super(message);
        this.retryAfter = retryAfter;
        this.type = type;
        this.server = server;
        this.channel = channel;
        this.manager = manager;
    }

    /**
     * Gets the type of the rate limit.
     *
     * @return The type of the rate limit.
     */
    public RateLimitType getType() {
        return type;
    }

    /**
     * Gets the server of the rate limit.
     *
     * @return The server of the rate limit. Can be <code>null</code> for non-server related limits.
     */
    public Server getServer() {
        return server;
    }

    /**
     * Gets the "retry_after" received in the response.
     * Retry after is the time in milliseconds we have to wait for the next request.
     * NOTE: The value does not get updated!
     *
     * @return The "retry_after" received in the response.
     */
    public long getRetryAfter() {
        return retryAfter;
    }

    /**
     * The calculated time when we can send a new messages.
     *
     * @return The calculated time when we can send a new message.
     */
    public long getRetryAt() {
        return System.currentTimeMillis() +  manager.getRateLimit(type, server, channel);
    }

    /**
     * Causes the current thread to wait until we can retry the request.
     *
     * @throws InterruptedException if we got interrupted.
     */
    public void waitTillRetry() throws InterruptedException {
        long time = manager.getRateLimit(type, server, channel);
        if (time < 1) {
            return;
        }
        Thread.sleep(time);
    }

}
