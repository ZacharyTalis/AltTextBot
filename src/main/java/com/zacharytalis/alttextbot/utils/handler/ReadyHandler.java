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
package com.zacharytalis.alttextbot.utils.handler;

import com.zacharytalis.alttextbot.ImplDiscordAPI;
import com.zacharytalis.alttextbot.entities.User;
import com.zacharytalis.alttextbot.entities.impl.ImplServer;
import com.zacharytalis.alttextbot.entities.impl.ImplUser;
import com.zacharytalis.alttextbot.utils.PacketHandler;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This class handles the ready packet.
 */
public class ReadyHandler extends PacketHandler {

    /**
     * Creates a new instance of this class.
     *
     * @param api The api.
     */
    public ReadyHandler(ImplDiscordAPI api) {
        super(api, false, "READY");
    }

    @Override
    public void handle(JSONObject packet) {
        JSONArray guilds = packet.getJSONArray("guilds"); // guild = server
        for (int i = 0; i < guilds.length(); i++) {
            JSONObject guild = guilds.getJSONObject(i);
            if (guild.has("unavailable") && guild.getBoolean("unavailable")) {
                // add guild to the list of unavailable servers
                api.getUnavailableServers().add(guild.getString("id"));
                continue;
            }
            new ImplServer(guild, api);
        }

        JSONArray privateChannels = packet.getJSONArray("private_channels");
        for (int i = 0; i < privateChannels.length(); i++) {
            JSONObject privateChannel = privateChannels.getJSONObject(i);
            String id = privateChannel.getString("id");
            if (privateChannel.has("recipient")) {
                User user = api.getOrCreateUser(privateChannel.getJSONObject("recipient"));
                if (user != null) {
                    ((ImplUser) user).setUserChannelId(id);
                }
            }
        }

        api.setYourself(api.getOrCreateUser(packet.getJSONObject("user")));
    }

}
