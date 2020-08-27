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
package com.zacharytalis.alttextbot.listener.user;

import com.zacharytalis.alttextbot.DiscordAPI;
import com.zacharytalis.alttextbot.entities.Channel;
import com.zacharytalis.alttextbot.entities.User;
import com.zacharytalis.alttextbot.entities.VoiceChannel;
import com.zacharytalis.alttextbot.entities.permissions.Permissions;
import com.zacharytalis.alttextbot.listener.Listener;

/**
 * This listener listens to user permission changes.
 */
public interface UserChangeOverwrittenPermissionsListener extends Listener {

    /**
     * This method is called every time a user changed its overwritten permissions.
     *
     * @param api The api.
     * @param user The user with the updated permissions.
     * @param channel The channel with the updated permissions.
     * @param oldPermissions The old permissions of the user.
     */
    public void onUserChangeOverwrittenPermissions(
            DiscordAPI api, User user, Channel channel, Permissions oldPermissions);

    /**
     * This method is called every time a user changed its overwritten permissions.
     *
     * @param api The api.
     * @param user The user with the updated permissions.
     * @param channel The voice channel with the updated permissions.
     * @param oldPermissions The old permissions of the user.
     */
    public void onUserChangeOverwrittenPermissions(
            DiscordAPI api, User user, VoiceChannel channel, Permissions oldPermissions);

}
