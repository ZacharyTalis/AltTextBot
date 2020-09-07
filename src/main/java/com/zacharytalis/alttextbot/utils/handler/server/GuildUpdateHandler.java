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
package com.zacharytalis.alttextbot.utils.handler.server;

import com.zacharytalis.alttextbot.ImplDiscordAPI;
import com.zacharytalis.alttextbot.entities.Region;
import com.zacharytalis.alttextbot.entities.impl.ImplServer;
import com.zacharytalis.alttextbot.listener.server.ServerChangeIconListener;
import com.zacharytalis.alttextbot.listener.server.ServerChangeNameListener;
import com.zacharytalis.alttextbot.listener.server.ServerChangeOwnerListener;
import com.zacharytalis.alttextbot.listener.server.ServerChangeRegionListener;
import com.zacharytalis.alttextbot.utils.LoggerUtil;
import com.zacharytalis.alttextbot.utils.PacketHandler;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.List;

/**
 * Handles the guild update packet.
 */
public class GuildUpdateHandler extends PacketHandler {

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerUtil.getLogger(GuildUpdateHandler.class);

    /**
     * Creates a new instance of this class.
     *
     * @param api The api.
     */
    public GuildUpdateHandler(ImplDiscordAPI api) {
        super(api, true, "GUILD_UPDATE");
    }

    @Override
    public void handle(JSONObject packet) {
        if (packet.has("unavailable") && packet.getBoolean("unavailable")) {
            return;
        }
        final ImplServer server = (ImplServer) api.getServerById(packet.getString("id"));

        String name = packet.getString("name");
        if (!server.getName().equals(name)) {
            final String oldName = server.getName();
            server.setName(name);
            listenerExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    List<ServerChangeNameListener> listeners = api.getListeners(ServerChangeNameListener.class);
                    synchronized (listeners) {
                        for (ServerChangeNameListener listener : listeners) {
                            try {
                                listener.onServerChangeName(api, server, oldName);
                            } catch (Exception t) {
                                logger.warn("Uncaught exception in ServerChangeNameListener!", t);
                            }
                        }
                    }
                }
            });
        }

        Region region = Region.getRegionByKey(packet.getString("region"));
        if (server.getRegion() != region) {
            final Region oldRegion = server.getRegion();
            server.setRegion(region);
            listenerExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    List<ServerChangeRegionListener> listeners = api.getListeners(ServerChangeRegionListener.class);
                    synchronized (listeners) {
                        for (ServerChangeRegionListener listener : listeners) {
                            try {
                                listener.onServerChangeRegion(api, server, oldRegion);
                            } catch (Exception t) {
                                logger.warn("Uncaught exception in ServerChangeRegionListener!", t);
                            }
                        }
                    }
                }
            });
        }

        String ownerId = packet.getString("owner_id");
        if (!server.getOwnerId().equals(ownerId)) {
            final String oldOwnerId = server.getOwnerId();
            server.setOwnerId(ownerId);
            listenerExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    List<ServerChangeOwnerListener> listeners = api.getListeners(ServerChangeOwnerListener.class);
                    synchronized (listeners) {
                        for (ServerChangeOwnerListener listener : listeners) {
                            try {
                                listener.onServerChangeOwner(api, server, oldOwnerId);
                            } catch (Exception t) {
                                logger.warn("Uncaught exception in ServerChangeOwnerListener!", t);
                            }
                        }
                    }
                }
            });
        }

        String icon = packet.isNull("icon") ? null : packet.getString("icon");
        if (server.getIconHash() != null && !server.getIconHash().equals(icon)) {
            final String oldIcon = server.getIconHash();
            server.setIconHash(icon);
            listenerExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    List<ServerChangeIconListener> listeners = api.getListeners(ServerChangeIconListener.class);
                    synchronized (listeners) {
                        for (ServerChangeIconListener listener : listeners) {
                            try {
                                listener.onServerChangeIcon(api, server, oldIcon);
                            } catch (Exception t) {
                                logger.warn("Uncaught exception in ServerChangeIconListener!", t);
                            }
                        }
                    }
                }
            });
        } else if(server.getIconHash() == null && server.getIconHash() != icon) {
            final String oldIcon = server.getIconHash();
            server.setIconHash(icon);
            listenerExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    List<ServerChangeIconListener> listeners = api.getListeners(ServerChangeIconListener.class);
                    synchronized (listeners) {
                        for (ServerChangeIconListener listener : listeners) {
                            try {
                                listener.onServerChangeIcon(api, server, oldIcon);
                            } catch (Exception t) {
                                logger.warn("Uncaught exception in ServerChangeIconListener!", t);
                            }
                        }
                    }
                }
            });
        }
    }

}
