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
import com.zacharytalis.alttextbot.entities.VoiceChannel;
import com.zacharytalis.alttextbot.entities.impl.ImplServer;
import com.zacharytalis.alttextbot.listener.channel.ChannelDeleteListener;
import com.zacharytalis.alttextbot.listener.voicechannel.VoiceChannelDeleteListener;
import com.zacharytalis.alttextbot.utils.LoggerUtil;
import com.zacharytalis.alttextbot.utils.PacketHandler;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.List;

/**
 * Handles the channel delete packet.
 */
public class ChannelDeleteHandler extends PacketHandler {

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerUtil.getLogger(ChannelDeleteHandler.class);

    /**
     * Creates a new instance of this class.
     *
     * @param api The api.
     */
    public ChannelDeleteHandler(ImplDiscordAPI api) {
        super(api, true, "CHANNEL_DELETE");
    }

    @Override
    public void handle(JSONObject packet) {
        int type = packet.getInt("type");
        if (type == 0) {
            handleServerTextChannel(packet, api.getServerById(packet.getString("guild_id")));
        } else if (type == 2) {
            handleServerVoiceChannel(packet, api.getServerById(packet.getString("guild_id")));
        }
    }

    /**
     * Handles the server text channels.
     *
     * @param packet The packet (the "d"-object).
     * @param server The server of the channel.
     */
    private void handleServerTextChannel(JSONObject packet, Server server) {
        final Channel channel = server.getChannelById(packet.getString("id"));
        if (channel == null) {
            return;
        }
        ((ImplServer) server).removeChannel(channel);
        listenerExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                List<ChannelDeleteListener> listeners = api.getListeners(ChannelDeleteListener.class);
                synchronized (listeners) {
                    for (ChannelDeleteListener listener : listeners) {
                        try {
                            listener.onChannelDelete(api, channel);
                        } catch (Throwable t) {
                            logger.warn("Uncaught exception in ChannelDeleteListener!", t);
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
        final VoiceChannel channel = server.getVoiceChannelById(packet.getString("id"));
        if (channel == null) {
            return;
        }
        ((ImplServer) server).removeVoiceChannel(channel);
        listenerExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                List<VoiceChannelDeleteListener> listeners = api.getListeners(VoiceChannelDeleteListener.class);
                synchronized (listeners) {
                    for (VoiceChannelDeleteListener listener : listeners) {
                        try {
                            listener.onVoiceChannelDelete(api, channel);
                        } catch (Throwable t) {
                            logger.warn("Uncaught exception in VoiceChannelDeleteListener!", t);
                        }
                    }
                }
            }
        });
    }

}
