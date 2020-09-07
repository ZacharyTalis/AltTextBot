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
package com.zacharytalis.alttextbot.entities.impl;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.zacharytalis.alttextbot.ImplDiscordAPI;
import com.zacharytalis.alttextbot.entities.InviteBuilder;
import com.zacharytalis.alttextbot.entities.Server;
import com.zacharytalis.alttextbot.entities.User;
import com.zacharytalis.alttextbot.entities.VoiceChannel;
import com.zacharytalis.alttextbot.entities.permissions.Permissions;
import com.zacharytalis.alttextbot.entities.permissions.Role;
import com.zacharytalis.alttextbot.entities.permissions.impl.ImplPermissions;
import com.zacharytalis.alttextbot.entities.permissions.impl.ImplRole;
import com.zacharytalis.alttextbot.listener.voicechannel.VoiceChannelChangeNameListener;
import com.zacharytalis.alttextbot.listener.voicechannel.VoiceChannelChangePositionListener;
import com.zacharytalis.alttextbot.listener.voicechannel.VoiceChannelDeleteListener;
import com.zacharytalis.alttextbot.utils.LoggerUtil;
import com.zacharytalis.alttextbot.utils.SnowflakeUtil;
import com.zacharytalis.alttextbot.utils.ratelimits.RateLimitType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * The implementation of the voice channel interface.
 */
public class ImplVoiceChannel implements VoiceChannel {

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerUtil.getLogger(ImplVoiceChannel.class);

    private static final Permissions emptyPermissions = new ImplPermissions(0, 0);

    private final ImplDiscordAPI api;

    private final String id;
    private String name;
    private int position;
    private final ImplServer server;
    private String parentId = null;

    private final ConcurrentHashMap<String, Permissions> overwrittenPermissions = new ConcurrentHashMap<>();

    private Set<User> connectedUsers = new HashSet<>();
    /**
     * Creates a new instance of this class.
     *
     * @param data A JSONObject containing all necessary data.
     * @param server The server of the channel.
     * @param api The api of this server.
     */
    public ImplVoiceChannel(JSONObject data, ImplServer server, ImplDiscordAPI api) {
        this.api = api;
        this.server = server;

        id = data.getString("id");
        name = data.getString("name");
        position = data.getInt("position");
        if (data.has("parent_id") && !data.isNull("parent_id")) {
            parentId = data.getString("parent_id");
        }

        JSONArray permissionOverwrites = data.getJSONArray("permission_overwrites");
        for (int i = 0; i < permissionOverwrites.length(); i++) {
            JSONObject permissionOverwrite = permissionOverwrites.getJSONObject(i);
            String id = permissionOverwrite.getString("id");
            int allow = permissionOverwrite.getInt("allow");
            int deny = permissionOverwrite.getInt("deny");
            String type = permissionOverwrite.getString("type");
            if (type.equals("role")) {
                Role role = server.getRoleById(id);
                if (role != null) {
                    ((ImplRole) role).setOverwrittenPermissions(this, new ImplPermissions(allow, deny));
                }
            }
            if (type.equals("member")) {
                overwrittenPermissions.put(id, new ImplPermissions(allow, deny));
            }
        }

        server.addVoiceChannel(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Calendar getCreationDate() {
        return SnowflakeUtil.parseDate(id);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public Future<Void> delete() {
        return api.getThreadPool().getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Trying to delete voice channel {}", ImplVoiceChannel.this);
                HttpResponse<JsonNode> response = Unirest
                        .delete("https://discordapp.com/api/v6/channels/" + id)
                        .header("authorization", api.getToken())
                        .asJson();
                api.checkResponse(response);
                api.checkRateLimit(response, RateLimitType.UNKNOWN, server, null);
                server.removeVoiceChannel(ImplVoiceChannel.this);
                logger.info("Deleted voice channel {}", ImplVoiceChannel.this);
                // call listener
                api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                    @Override
                    public void run() {
                        List<VoiceChannelDeleteListener> listeners =
                                api.getListeners(VoiceChannelDeleteListener.class);
                        synchronized (listeners) {
                            for (VoiceChannelDeleteListener listener : listeners) {
                                try {
                                    listener.onVoiceChannelDelete(api, ImplVoiceChannel.this);
                                } catch (Exception t) {
                                    logger.warn("Uncaught exception in VoiceChannelDeleteListener!", t);
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
    public InviteBuilder getInviteBuilder() {
        return new ImplInviteBuilder(this, api);
    }

    @Override
    public Permissions getOverwrittenPermissions(User user) {
        Permissions permissions = overwrittenPermissions.get(user.getId());
        return permissions == null ? emptyPermissions : permissions;
    }

    @Override
    public Permissions getOverwrittenPermissions(Role role) {
        return role.getOverwrittenPermissions(this);
    }

    @Override
    public Future<Void> updateOverwrittenPermissions(final Role role, final Permissions permissions) {
        return api.getThreadPool().getListeningExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Updating permissions in channel {} for role {} (allow: {}, deny: {})", this, role,
                        ((ImplPermissions) permissions).getAllowed(), ((ImplPermissions) permissions).getDenied());
                Unirest.put("https://discordapp.com/api/v6/channels/" + getId() + "/permissions/" + role.getId())
                        .header("authorization", api.getToken())
                        .header("Content-Type", "application/json")
                        .body(new JSONObject()
                                .put("allow", ((ImplPermissions) permissions).getAllowed())
                                .put("deny", ((ImplPermissions) permissions).getDenied())
                                .put("type", "role").toString())
                        .asJson();
                logger.debug("Updated permissions in channel {} for role {} (allow: {}, deny: {})", this, role,
                        ((ImplPermissions) permissions).getAllowed(), ((ImplPermissions) permissions).getDenied());
                return null;
            }
        });
    }

    @Override
    public Future<Void> updateOverwrittenPermissions(final User user, final Permissions permissions) {
        return api.getThreadPool().getListeningExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Updating permissions in channel {} for user {} (allow: {}, deny: {})", this, user,
                        ((ImplPermissions) permissions).getAllowed(), ((ImplPermissions) permissions).getDenied());
                Unirest.put("https://discordapp.com/api/v6/channels/" + getId() + "/permissions/" + user.getId())
                        .header("authorization", api.getToken())
                        .header("Content-Type", "application/json")
                        .body(new JSONObject()
                                .put("allow", ((ImplPermissions) permissions).getAllowed())
                                .put("deny", ((ImplPermissions) permissions).getDenied())
                                .put("type", "member").toString())
                        .asJson();
                logger.debug("Updated permissions in channel {} for user {} (allow: {}, deny: {})", this, user,
                        ((ImplPermissions) permissions).getAllowed(), ((ImplPermissions) permissions).getDenied());
                return null;
            }
        });
    }

    @Override
    public Future<Void> deleteOverwrittenPermissions(final Role role) {
        return api.getThreadPool().getListeningExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Deleting permissions in channel {} for role {}", this, role);
                Unirest.delete("https://discordapp.com/api/v6/channels/" + getId() + "/permissions/" + role.getId())
                        .header("authorization", api.getToken())
                        .asJson();
                logger.debug("Deleted permissions in channel {} for role {}", this, role);
                return null;
            }
        });
    }

    @Override
    public Future<Void> deleteOverwrittenPermissions(final User user) {
        return api.getThreadPool().getListeningExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Deleting permissions in channel {} for user {}", this, user);
                Unirest.delete("https://discordapp.com/api/v6/channels/" + getId() + "/permissions/" + user.getId())
                        .header("authorization", api.getToken())
                        .asJson();
                logger.debug("Deleted permissions in channel {} for user {}", this, user);
                return null;
            }
        });
    }

    @Override
    public Future<Void> updateName(String newName) {
        return update(newName, getPosition());
    }

    @Override
    public Future<Void> updatePosition(int newPosition) {
        return update(getName(), newPosition);
    }

    @Override
    public Future<Void> update(final String newName, final int newPosition) {
        final JSONObject params = new JSONObject()
                .put("name", newName)
                .put("position", newPosition);
        return api.getThreadPool().getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Trying to update channel {} (new name: {}, old name: {}, new position: {}, old position: {})",
                        ImplVoiceChannel.this, newName, getName(), newPosition, getPosition());
                HttpResponse<JsonNode> response = Unirest
                        .patch("https://discordapp.com/api/v6/channels/" + getId())
                        .header("authorization", api.getToken())
                        .header("Content-Type", "application/json")
                        .body(params.toString())
                        .asJson();
                api.checkResponse(response);
                api.checkRateLimit(response, RateLimitType.UNKNOWN, server, null);
                logger.info("Updated channel {} (new name: {}, old name: {}, new position: {}, old position: {})",
                        ImplVoiceChannel.this, newName, getName(), newPosition, getPosition());
                String updatedName = response.getBody().getObject().getString("name");
                int updatedPosition = response.getBody().getObject().getInt("position");

                // check name
                if (!updatedName.equals(getName())) {
                    final String oldName = getName();
                    setName(updatedName);
                    api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                        @Override
                        public void run() {
                            List<VoiceChannelChangeNameListener> listeners =
                                    api.getListeners(VoiceChannelChangeNameListener.class);
                            synchronized (listeners) {
                                for (VoiceChannelChangeNameListener listener : listeners) {
                                    try {
                                        listener.onVoiceChannelChangeName(api, ImplVoiceChannel.this, oldName);
                                    } catch (Exception t) {
                                        logger.warn("Uncaught exception in VoiceChannelChangeNameListener!", t);
                                    }
                                }
                            }
                        }
                    });
                }

                // check position
                if (updatedPosition != getPosition()) {
                    final int oldPosition = getPosition();
                    setPosition(updatedPosition);
                    api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                        @Override
                        public void run() {
                            List<VoiceChannelChangePositionListener> listeners =
                                    api.getListeners(VoiceChannelChangePositionListener.class);
                            synchronized (listeners) {
                                for (VoiceChannelChangePositionListener listener : listeners) {
                                    try {
                                        listener.onVoiceChannelChangePosition(api, ImplVoiceChannel.this, oldPosition);
                                    } catch (Exception t) {
                                        logger.warn("Uncaught exception in VoiceChannelChangePositionListener!", t);
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

    /**
     * Sets the name of the channel (no update!).
     *
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the position of the channel (no update!).
     *
     * @param position The position to set.
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Sets the overwritten permissions of an user.
     *
     * @param user The user which overwrites the permissions.
     * @param permissions The overwritten permissions.
     */
    public void setOverwrittenPermissions(User user, Permissions permissions) {
        overwrittenPermissions.put(user.getId(), permissions);
    }

    /**
     * Removes the overwritten permissions of a user from the cache.
     *
     *
     * @param user The user, which permissions should be removed.
     */
    public void removeOverwrittenPermissions(User user) {
        overwrittenPermissions.remove(user.getId());
    }

    /**
     * Adds a {@link User} to the set of connected Users.
     *
     * @param user
     *            The connected user to add.
     */
    public void addConnectedUser(User user) {
        this.connectedUsers.add(user);
    }

    /**
     * Removes a {@link User} from the set of connected Users.
     *
     * @param user
     *            The connected user to remove if found.
     */
    public void removeConnectedUser(User user) {
        this.connectedUsers.remove(user);
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    /**
     * Sets the parent id of the channel (no update!).
     *
     * @param parentId The id of the parent category
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /**
     * Returns a set of users connected to this channel.
     * 
     * @return the set of users connected to this channel.
     */
    @Override
    public final Set<User> getConnectedUsers() {
        return this.connectedUsers;
    }

    @Override
    public String toString() {
        return getName() + " (id: " + getId() + ")";
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
