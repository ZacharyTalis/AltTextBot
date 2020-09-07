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
package com.zacharytalis.alttextbot.entities.permissions.impl;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.zacharytalis.alttextbot.ImplDiscordAPI;
import com.zacharytalis.alttextbot.entities.Channel;
import com.zacharytalis.alttextbot.entities.Server;
import com.zacharytalis.alttextbot.entities.User;
import com.zacharytalis.alttextbot.entities.VoiceChannel;
import com.zacharytalis.alttextbot.entities.impl.ImplServer;
import com.zacharytalis.alttextbot.entities.permissions.Permissions;
import com.zacharytalis.alttextbot.entities.permissions.Role;
import com.zacharytalis.alttextbot.listener.role.*;
import com.zacharytalis.alttextbot.utils.LoggerUtil;
import com.zacharytalis.alttextbot.utils.ratelimits.RateLimitType;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * The implementation of the role interface.
 */
public class ImplRole implements Role {

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerUtil.getLogger(ImplRole.class);

    private static final Permissions emptyPermissions = new ImplPermissions(0, 0);

    // key = channelId
    private final ConcurrentHashMap<String, Permissions> overwrittenPermissions = new ConcurrentHashMap<>();

    private final ImplDiscordAPI api;

    private final String id;
    private String name;
    private final ImplServer server;
    private ImplPermissions permissions;
    private int position;
    private Color color;
    private boolean hoist;
    private boolean mentionable;
    private boolean managed;

    private final List<User> users = new ArrayList<>();

    /**
     * Creates a new instance of this class.
     *
     * @param data A JSONObject containing all necessary data.
     * @param server The server of the role.
     * @param api The api of this server.
     */
    public ImplRole(JSONObject data, ImplServer server, ImplDiscordAPI api) {
        this.server = server;
        this.api = api;

        id = data.getString("id");
        name = data.getString("name");
        permissions = new ImplPermissions(data.getInt("permissions"));
        position = data.getInt("position");
        color = new Color(data.getInt("color"));
        hoist = data.getBoolean("hoist");
        mentionable = data.getBoolean("mentionable");
        managed = data.getBoolean("managed");

        server.addRole(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public Permissions getPermissions() {
        return permissions;
    }

    @Override
    public Permissions getOverwrittenPermissions(Channel channel) {
        Permissions overwrittenPermissions = this.overwrittenPermissions.get(channel.getId());
        if (overwrittenPermissions == null) {
            overwrittenPermissions = emptyPermissions;
        }
        return overwrittenPermissions;
    }

    @Override
    public Permissions getOverwrittenPermissions(VoiceChannel channel) {
        Permissions overwrittenPermissions = this.overwrittenPermissions.get(channel.getId());
        if (overwrittenPermissions == null) {
            overwrittenPermissions = emptyPermissions;
        }
        return overwrittenPermissions;
    }

    @Override
    public List<User> getUsers() {
        return new ArrayList<>(users);
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public boolean getHoist() {
        return hoist;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public boolean isMentionable() {
        return mentionable;
    }

    @Override
    public boolean isManaged() {
        return managed;
    }

    @Override
    public String getMentionTag() {
        return "<@&" + getId() + ">";
    }

    @Override
    public Future<Void> updatePermissions(Permissions permissions) {
        return update(name, color, hoist, permissions, mentionable);
    }

    @Override
    public Future<Void> updateName(String name) {
        return update(name, color, hoist, permissions, mentionable);
    }

    @Override
    public Future<Void> updateColor(Color color) {
        return update(name, color, hoist, permissions, mentionable);
    }

    @Override
    public Future<Void> updateHoist(boolean hoist) {
        return update(name, color, hoist, permissions, mentionable);
    }
    
    @Override
    public Future<Void> updateMentionable(boolean mentionable) {
        return update(name, color, hoist, permissions, mentionable);
    }
    
    @Override
    public Future<Void> update(String name, Color color, boolean hoist, Permissions permissions, boolean mentionable) {
        if (name == null) {
            name = getName();
        }
        if (color == null) {
            color = getColor();
        }
        if (permissions == null) {
            permissions = getPermissions();
        }
        return update(name, color.getRGB(), hoist, ((ImplPermissions) permissions).getAllowed(), mentionable);
    }

    /**
     * Updates the role.
     *
     * @param name The new name of the role.
     * @param color The new color of the role.
     * @param hoist The new hoist of the role.
     * @param allow The new permissions of the role.
     * @param mentionable The new mentionable status of the role.
     * @return A future.
     */
    private Future<Void> update(final String name, final int color, final boolean hoist, final int allow, final boolean mentionable) {
        return api.getThreadPool().getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Trying to update role {} (new name: {}, old name: {}, new color: {}, old color: {}," +
                        " new hoist: {}, old hoist: {}, new allow: {}, old allow: {})",
                        ImplRole.this, name, getName(), color & 0xFFFFFF, getColor().getRGB() & 0xFFFFFF,
                        hoist, getHoist(), allow, permissions.getAllowed());
                HttpResponse<JsonNode> response = Unirest
                        .patch("https://discordapp.com/api/v6/guilds/" + server.getId() + "/roles/" + id)
                        .header("authorization", api.getToken())
                        .header("Content-Type", "application/json")
                        .body(new JSONObject()
                                .put("name", name)
                                .put("color", color & 0xFFFFFF)
                                .put("hoist", hoist)
                                .put("permissions", allow)
                                .put("mentionable", mentionable).toString())
                        .asJson();
                api.checkResponse(response);
                api.checkRateLimit(response, RateLimitType.UNKNOWN, null, null);

                logger.info("Updated role {} (new name: {}, old name: {}, new color: {}, old color: {}," +
                                " new hoist: {}, old hoist: {}, new allow: {}, old allow: {})",
                        ImplRole.this, name, getName(), color & 0xFFFFFF, getColor().getRGB() & 0xFFFFFF,
                        hoist, getHoist(), allow, permissions.getAllowed());
                // update permissions
                if (ImplRole.this.permissions.getAllowed() != allow) {
                    final ImplPermissions oldPermissions = ImplRole.this.permissions;
                    ImplRole.this.permissions = new ImplPermissions(allow);
                    // call listener
                    api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                        @Override
                        public void run() {
                            List<RoleChangePermissionsListener> listeners =
                                    api.getListeners(RoleChangePermissionsListener.class);
                            synchronized (listeners) {
                                for (RoleChangePermissionsListener listener : listeners) {
                                    try {
                                        listener.onRoleChangePermissions(api, ImplRole.this, oldPermissions);
                                    } catch (Exception t) {
                                        logger.warn("Uncaught exception in RoleChangePermissionsListener!", t);
                                    }
                                }
                            }
                        }
                    });
                }

                // update name
                if (ImplRole.this.name.equals(name)) {
                    final String oldName = ImplRole.this.name;
                    ImplRole.this.name = name;
                    // call listener
                    api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                        @Override
                        public void run() {
                            List<RoleChangeNameListener> listeners = api.getListeners(RoleChangeNameListener.class);
                            synchronized (listeners) {
                                for (RoleChangeNameListener listener : listeners) {
                                    try {
                                        listener.onRoleChangeName(api, ImplRole.this, oldName);
                                    } catch (Exception t) {
                                        logger.warn("Uncaught exception in RoleChangeNameListener!", t);
                                    }
                                }
                            }
                        }
                    });
                }

                // update color
                if (ImplRole.this.color.getRGB() != new Color(color).getRGB()) {
                    final Color oldColor = ImplRole.this.color;
                    ImplRole.this.color = new Color(color);
                    // call listener
                    api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                        @Override
                        public void run() {
                            List<RoleChangeColorListener> listeners =
                                    api.getListeners(RoleChangeColorListener.class);
                            synchronized (listeners) {
                                for (RoleChangeColorListener listener : listeners) {
                                    try {
                                        listener.onRoleChangeColor(api, ImplRole.this, oldColor);
                                    } catch (Exception t) {
                                        logger.warn("Uncaught exception in RoleChangeColorListener!", t);
                                    }
                                }
                            }
                        }
                    });
                }

                // update hoist
                if (ImplRole.this.hoist != hoist) {
                    ImplRole.this.hoist = hoist;
                    // call listener
                    api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                        @Override
                        public void run() {
                            List<RoleChangeHoistListener> listeners =
                                    api.getListeners(RoleChangeHoistListener.class);
                            synchronized (listeners) {
                                for (RoleChangeHoistListener listener : listeners) {
                                    try {
                                        listener.onRoleChangeHoist(api, ImplRole.this, !ImplRole.this.hoist);
                                    } catch (Exception t) {
                                        logger.warn("Uncaught exception in RoleChangeHoistListener!", t);
                                    }
                                }
                            }
                        }
                    });
                }
                // update managed
                if (ImplRole.this.managed != managed) {
                    ImplRole.this.managed = managed;
                    // call listener
                    api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                        @Override
                        public void run() {
                            List<RoleChangeManagedListener> listeners =
                                    api.getListeners(RoleChangeManagedListener.class);
                            synchronized (listeners) {
                                for (RoleChangeManagedListener listener : listeners) {
                                    try {
                                        listener.onRoleChangeManaged(api, ImplRole.this, !ImplRole.this.managed);
                                    } catch (Exception t) {
                                        logger.warn("Uncaught exception in RoleChangeManagedListener!", t);
                                    }
                                }
                            }
                        }
                    });
                }
                // update mentionable
                if (ImplRole.this.mentionable != mentionable) {
                    ImplRole.this.mentionable = mentionable;
                    // call listener
                    api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                        @Override
                        public void run() {
                            List<RoleChangeMentionableListener> listeners =
                                    api.getListeners(RoleChangeMentionableListener.class);
                            synchronized (listeners) {
                                for (RoleChangeMentionableListener listener : listeners) {
                                    try {
                                        listener.onRoleChangeMentionable(api, ImplRole.this, !ImplRole.this.mentionable);
                                    } catch (Exception t) {
                                        logger.warn("Uncaught exception in RoleChangeMentionableListener!", t);
                                    }
                                }
                            }
                        }
                    });
                }
                return null;
            }
        });
    }

    @Override
    public Future<Void> delete() {
        return api.getThreadPool().getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Trying to delete role {}", ImplRole.this);
                HttpResponse<JsonNode> response = Unirest
                        .delete("https://discordapp.com/api/v6/guilds/" + getServer().getId() + "/roles/" + getId())
                        .header("authorization", api.getToken())
                        .asJson();
                api.checkResponse(response);
                server.removeRole(ImplRole.this);
                logger.info("Deleted role {}", ImplRole.this);
                api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                    @Override
                    public void run() {
                        List<RoleDeleteListener> listeners = api.getListeners(RoleDeleteListener.class);
                        synchronized (listeners) {
                            for (RoleDeleteListener listener : listeners) {
                                try {
                                    listener.onRoleDelete(api, ImplRole.this);
                                } catch (Exception t) {
                                    logger.warn("Uncaught exception in RoleDeleteListener!", t);
                                }
                            }
                        }
                    }
                });
                return null;
            }
        });
    }

    @Override
    public Future<Void> removeUser(User user) {
        List<Role> roles = new ArrayList<>(user.getRoles(getServer()));
        roles.remove(this);
        return getServer().updateRoles(user, roles.toArray(new Role[roles.size()]));
    }

    @Override
    public Future<Void> addUser(User user) {
        List<Role> roles = new ArrayList<>(user.getRoles(getServer()));
        roles.add(this);
        return getServer().updateRoles(user, roles.toArray(new Role[roles.size()]));
    }

    /**
     * Adds an user.
     *
     * @param user The user to add.
     */
    public void addUserNoUpdate(User user) {
        synchronized (users) {
            users.add(user);
        }
    }

    /**
     * Removes an user.
     *
     * @param user The user to remove.
     */
    public void removeUserNoUpdate(User user) {
        synchronized (users) {
            users.remove(user);
        }
    }

    /**
     * Sets the name of the channel.
     *
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the permissions of the channel.
     *
     * @param permissions The permissions to set.
     */
    public void setPermissions(ImplPermissions permissions) {
        this.permissions = permissions;
    }

    /**
     * Sets the position of the channel.
     *
     * @param position The position to set.
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Sets overwritten permissions.
     *
     * @param channel The channel which overwrites the permissions.
     * @param permissions The overwritten permissions to set.
     */
    public void setOverwrittenPermissions(Channel channel, Permissions permissions) {
        overwrittenPermissions.put(channel.getId(), permissions);
    }

    /**
     * Sets overwritten permissions.
     *
     * @param channel The voice channel which overwrites the permissions.
     * @param permissions The overwritten permissions to set.
     */
    public void setOverwrittenPermissions(VoiceChannel channel, Permissions permissions) {
        overwrittenPermissions.put(channel.getId(), permissions);
    }

    /**
     * Sets the color of the channel.
     *
     * @param color The color to set.
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Sets the hoist of the channel.
     *
     * @param hoist The hoist to set.
     */
    public void setHoist(boolean hoist) {
        this.hoist = hoist;
    }
    
    /**
     * Sets the mentionable of the role
     * 
     * @param mentionable The mentionable to set
     */
    public void setMentionable(boolean mentionable) {
        this.mentionable = mentionable;
    }
    
    /**
     * Sets the managed of the role
     * 
     * @param managed The managed to set
     */
    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    @Override
    public String toString() {
        return getName() + " (id: " + getId() + ")";
    }
}
