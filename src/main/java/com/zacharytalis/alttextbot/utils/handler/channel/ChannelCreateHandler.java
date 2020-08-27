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
package com.zacharytalis.alttextbot.utils.handler.channel;

import com.zacharytalis.alttextbot.ImplDiscordAPI;
import com.zacharytalis.alttextbot.entities.Channel;
import com.zacharytalis.alttextbot.entities.Server;
import com.zacharytalis.alttextbot.entities.User;
import com.zacharytalis.alttextbot.entities.VoiceChannel;
import com.zacharytalis.alttextbot.entities.impl.ImplChannel;
import com.zacharytalis.alttextbot.entities.impl.ImplServer;
import com.zacharytalis.alttextbot.entities.impl.ImplUser;
import com.zacharytalis.alttextbot.entities.impl.ImplVoiceChannel;
import com.zacharytalis.alttextbot.listener.channel.ChannelCreateListener;
import com.zacharytalis.alttextbot.listener.voicechannel.VoiceChannelCreateListener;
import com.zacharytalis.alttextbot.utils.LoggerUtil;
import com.zacharytalis.alttextbot.utils.PacketHandler;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.List;

/**
 * Handles the channel create packet.
 */
public class ChannelCreateHandler extends PacketHandler {

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerUtil.getLogger(ChannelCreateHandler.class);

    /**
     * Creates a new instance of this class.
     *
     * @param api The api.
     */
    public ChannelCreateHandler(ImplDiscordAPI api) {
        super(api, true, "CHANNEL_CREATE");
    }

    @Override
    public void handle(JSONObject packet) {
        int type = packet.getInt("type");
        switch (type) {
            case 0:
                handleServerTextChannel(packet, api.getServerById(packet.getString("guild_id")));
                break;
            case 1:
                User recipient = api.getOrCreateUser(packet.getJSONArray("recipients").getJSONObject(0));
                ((ImplUser) recipient).setUserChannelId(packet.getString("id"));
                break;
            case 2:
                handleServerVoiceChannel(packet, api.getServerById(packet.getString("guild_id")));
                break;
            case 3:
                // TODO DM groups
                break;
            case 4:
                break;
            default:
                break;
        }
    }

    /**
     * Handles the server text channels.
     *
     * @param packet The packet (the "d"-object).
     * @param server The server of the channel.
     */
    private void handleServerTextChannel(JSONObject packet, Server server) {
        if (server.getChannelById(packet.getString("id")) != null) {
            return;
        }
        final Channel channel = new ImplChannel(packet, (ImplServer) server, api);
        listenerExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                List<ChannelCreateListener> listeners = api.getListeners(ChannelCreateListener.class);
                synchronized (listeners) {
                    for (ChannelCreateListener listener : listeners) {
                        try {
                            listener.onChannelCreate(api, channel);
                        } catch (Throwable t) {
                            logger.warn("Uncaught exception in ChannelCreateListener!", t);
                        }
                    }
                }
            }
        });
    }

    /**
     * Handles the server voice channels.
     *
     * @param packet The packet (the "d"-object).
     * @param server The server of the channel.
     */
    private void handleServerVoiceChannel(JSONObject packet, Server server) {
        if (server.getVoiceChannelById(packet.getString("id")) != null) {
            return;
        }
        final VoiceChannel channel = new ImplVoiceChannel(packet, (ImplServer) server, api);
        listenerExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                List<VoiceChannelCreateListener> listeners = api.getListeners(VoiceChannelCreateListener.class);
                synchronized (listeners) {
                    for (VoiceChannelCreateListener listener : listeners) {
                        try {
                            listener.onVoiceChannelCreate(api, channel);
                        } catch (Throwable t) {
                            logger.warn("Uncaught exception in VoiceChannelCreateListener!", t);
                        }
                    }
                }
            }
        });
    }

}
