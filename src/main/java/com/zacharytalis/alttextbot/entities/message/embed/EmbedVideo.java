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
package com.zacharytalis.alttextbot.entities.message.embed;

import java.net.URL;

/**
 * This interface represents an embed video.
 */
public interface EmbedVideo {

    /**
     * Gets the url of the video.
     *
     * @return The url of the video.
     */
    public URL getUrl();

    /**
     * Gets the height of the video.
     *
     * @return The height of the video.
     */
    public int getHeight();

    /**
     * Gets the width of the video.
     *
     * @return The width of the video.
     */
    public int getWidth();

}
