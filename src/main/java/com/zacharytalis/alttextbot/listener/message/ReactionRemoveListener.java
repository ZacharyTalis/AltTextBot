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
package com.zacharytalis.alttextbot.listener.message;

import com.zacharytalis.alttextbot.DiscordAPI;
import com.zacharytalis.alttextbot.entities.User;
import com.zacharytalis.alttextbot.entities.message.Reaction;
import com.zacharytalis.alttextbot.listener.Listener;

/**
 * This listener listens to reaction removes.
 */
public interface ReactionRemoveListener extends Listener {

    /**
     * This method is called every time a reaction was removed.
     *
     * @param api The api.
     * @param reaction The updated reaction. May have a count of <code>null</code> and no longer present in the message.
     * @param user The user who removed his reaction.
     */
    public void onReactionRemove(DiscordAPI api, Reaction reaction, User user);

}
