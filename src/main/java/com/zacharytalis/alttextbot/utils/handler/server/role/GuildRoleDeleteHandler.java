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
package com.zacharytalis.alttextbot.utils.handler.server.role;

import com.zacharytalis.alttextbot.ImplDiscordAPI;
import com.zacharytalis.alttextbot.entities.Server;
import com.zacharytalis.alttextbot.entities.impl.ImplServer;
import com.zacharytalis.alttextbot.entities.permissions.Role;
import com.zacharytalis.alttextbot.listener.role.RoleDeleteListener;
import com.zacharytalis.alttextbot.utils.LoggerUtil;
import com.zacharytalis.alttextbot.utils.PacketHandler;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.List;

/**
 * Handles the guild role delete packet.
 */
public class GuildRoleDeleteHandler extends PacketHandler {

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerUtil.getLogger(GuildRoleDeleteHandler.class);

    /**
     * Creates a new instance of this class.
     *
     * @param api The api.
     */
    public GuildRoleDeleteHandler(ImplDiscordAPI api) {
        super(api, true, "GUILD_ROLE_DELETE");
    }

    @Override
    public void handle(JSONObject packet) {
        String guildId = packet.getString("guild_id");
        String roleId = packet.getString("role_id");

        Server server = api.getServerById(guildId);
        final Role role = server.getRoleById(roleId);

        if (role == null) {
            return;
        }

        ((ImplServer) server).removeRole(role);

        listenerExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                List<RoleDeleteListener> listeners = api.getListeners(RoleDeleteListener.class);
                synchronized (listeners) {
                    for (RoleDeleteListener listener : listeners) {
                        try {
                            listener.onRoleDelete(api, role);
                        } catch (Throwable t) {
                            logger.warn("Uncaught exception in RoleDeleteListener!", t);
                        }
                    }
                }
            }
        });
    }

}
