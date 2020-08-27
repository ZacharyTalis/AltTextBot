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
import com.zacharytalis.alttextbot.entities.User;
import com.zacharytalis.alttextbot.entities.impl.ImplServer;
import com.zacharytalis.alttextbot.entities.permissions.Role;
import com.zacharytalis.alttextbot.entities.permissions.impl.ImplRole;
import com.zacharytalis.alttextbot.listener.user.UserChangeNicknameListener;
import com.zacharytalis.alttextbot.listener.user.UserRoleAddListener;
import com.zacharytalis.alttextbot.listener.user.UserRoleRemoveListener;
import com.zacharytalis.alttextbot.utils.LoggerUtil;
import com.zacharytalis.alttextbot.utils.PacketHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.List;

/**
 * Handles the guild member update packet.
 */
public class GuildMemberUpdateHandler extends PacketHandler {

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerUtil.getLogger(GuildMemberUpdateHandler.class);

    /**
     * Creates a new instance of this class.
     *
     * @param api The api.
     */
    public GuildMemberUpdateHandler(ImplDiscordAPI api) {
        super(api, true, "GUILD_MEMBER_UPDATE");
    }

    @Override
    public void handle(JSONObject packet) {
        final ImplServer server = (ImplServer) api.getServerById(packet.getString("guild_id"));
        final User user = api.getOrCreateUser(packet.getJSONObject("user"));
        if (server != null) {
            // update nickname
            if (packet.has("nick")) {
                String newNick = packet.isNull("nick") ? null : packet.getString("nick");
                final String oldNick = server.getNickname(user);
                if (newNick != null && !newNick.equals(oldNick) || ((oldNick != null) && !oldNick.equals(newNick))) {
                    server.setNickname(user, newNick);
                    listenerExecutorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            List<UserChangeNicknameListener> listeners = api.getListeners(UserChangeNicknameListener.class);
                            synchronized (listeners) {
                                for (UserChangeNicknameListener listener : listeners) {
                                    try {
                                        listener.onUserChangeNickname(api, server, user, oldNick);
                                    } catch (Throwable t) {
                                        logger.warn("Uncaught exception in UserChangeNicknameListener!", t);
                                    }
                                }
                            }
                        }
                    });
                }
            }

            // get array with all roles
            JSONArray jsonRoles = packet.getJSONArray("roles");
            Role[] roles = new Role[jsonRoles.length()];
            for (int i = 0; i < jsonRoles.length(); i++) {
                roles[i] = server.getRoleById(jsonRoles.getString(i));
            }

            // iterate throw all current roles and remove roles which aren't in the roles array
            for (final Role role : user.getRoles(server)) {
                boolean contains = false;
                for (Role r : roles) {
                    if (role == r) {
                        contains = true;
                        break;
                    }
                }
                if (!contains) {
                    ((ImplRole) role).removeUserNoUpdate(user);
                    api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                        @Override
                        public void run() {
                            List<UserRoleRemoveListener> listeners = api.getListeners(UserRoleRemoveListener.class);
                            synchronized (listeners) {
                                for (UserRoleRemoveListener listener : listeners) {
                                    try {
                                        listener.onUserRoleRemove(api, user, role);
                                    } catch (Throwable t) {
                                        logger.warn("Uncaught exception in UserRoleRemoveListenerListener!", t);
                                    }
                                }
                            }
                        }
                    });
                }
            }

            // iterate throw all roles of the roles array and remove add roles which aren't in the current roles list
            for (final Role role : roles) {
                if (!user.getRoles(server).contains(role)) {
                    ((ImplRole) role).addUserNoUpdate(user);
                    api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                        @Override
                        public void run() {
                            List<UserRoleAddListener> listeners = api.getListeners(UserRoleAddListener.class);
                            synchronized (listeners) {
                                for (UserRoleAddListener listener : listeners) {
                                    try {
                                        listener.onUserRoleAdd(api, user, role);
                                    } catch (Throwable t) {
                                        logger.warn("Uncaught exception in UserRoleAddListener!", t);
                                    }
                                }
                            }
                        }
                    });
                }
            }
        }
    }

}
