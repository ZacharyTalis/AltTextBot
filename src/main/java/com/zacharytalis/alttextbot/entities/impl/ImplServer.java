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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.zacharytalis.alttextbot.ImplDiscordAPI;
import com.zacharytalis.alttextbot.Javacord;
import com.zacharytalis.alttextbot.entities.*;
import com.zacharytalis.alttextbot.entities.permissions.Ban;
import com.zacharytalis.alttextbot.entities.permissions.Role;
import com.zacharytalis.alttextbot.entities.permissions.impl.ImplBan;
import com.zacharytalis.alttextbot.entities.permissions.impl.ImplRole;
import com.zacharytalis.alttextbot.listener.channel.ChannelCreateListener;
import com.zacharytalis.alttextbot.listener.role.RoleCreateListener;
import com.zacharytalis.alttextbot.listener.server.*;
import com.zacharytalis.alttextbot.listener.user.UserRoleAddListener;
import com.zacharytalis.alttextbot.listener.user.UserRoleRemoveListener;
import com.zacharytalis.alttextbot.listener.voicechannel.VoiceChannelCreateListener;
import com.zacharytalis.alttextbot.utils.LoggerUtil;
import com.zacharytalis.alttextbot.utils.SnowflakeUtil;
import com.zacharytalis.alttextbot.utils.ratelimits.RateLimitType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * The implementation of the server interface.
 */
public class ImplServer implements Server {

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerUtil.getLogger(ImplServer.class);

    private final ImplDiscordAPI api;

    private final ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, VoiceChannel> voiceChannels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> members = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Role> roles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CustomEmoji> customEmojis = new ConcurrentHashMap<>();
    // key = user id; value = user nickname
    private final ConcurrentHashMap<String, String> nicknames = new ConcurrentHashMap<>();

    private final String id;
    private String name;
    private Region region;
    private int memberCount;
    private final boolean large;
    private String ownerId;
    private String iconHash;

    /**
     * Creates a new instance of this class.
     *
     * @param data A JSONObject containing all necessary data.
     * @param api The api of this server.
     */
    public ImplServer(JSONObject data, ImplDiscordAPI api) {
        this.api = api;

        name = data.getString("name");
        id = data.getString("id");
        region = Region.getRegionByKey(data.getString("region"));
        memberCount = data.getInt("member_count");
        large = data.getBoolean("large");
        ownerId = data.getString("owner_id");

        JSONArray roles = data.getJSONArray("roles");
        for (int i = 0; i < roles.length(); i++) {
            new ImplRole(roles.getJSONObject(i), this, api);
        }

        JSONArray emojis = data.getJSONArray("emojis");
        for (int i = 0; i < emojis.length(); i++) {
            new ImplCustomEmoji(emojis.getJSONObject(i), this, api);
        }

        JSONArray channels = data.getJSONArray("channels");
        for (int i = 0; i < channels.length(); i++) {
            JSONObject channelJson = channels.getJSONObject(i);
            int type = channelJson.getInt("type");
            if (type == 0) {
                new ImplChannel(channels.getJSONObject(i), this, api);
            }
            if (type == 2) {
                new ImplVoiceChannel(channels.getJSONObject(i), this, api);
            }
        }

        JSONArray members = new JSONArray();
        if (data.has("members")) {
            members = data.getJSONArray("members");
        }
        addMembers(members);

        if (!api.isLazyLoading() && isLarge() && getMembers().size() < getMemberCount()) {
            JSONObject requestGuildMembersPacket = new JSONObject()
                    .put("op", 8)
                    .put("d", new JSONObject()
                            .put("guild_id", getId())
                            .put("query","")
                            .put("limit", 0));
            logger.debug("Sending request guild members packet for server {}", this);
            api.getSocketAdapter().getWebSocket().sendText(requestGuildMembersPacket.toString());
        }

        JSONArray voiceStates = new JSONArray();
        if (data.has("voice_states")) {
            voiceStates = data.getJSONArray("voice_states");
        }
        for (int i = 0; i < voiceStates.length(); ++i) {
            JSONObject voiceState = voiceStates.getJSONObject(i);
            if (!voiceState.has("user_id") || voiceState.isNull("user_id")) {
                continue;
            }
            User user = api.getCachedUserById(voiceState.getString("user_id"));
            if (user == null) {
                continue;
            }
            if (!voiceState.has("channel_id") || voiceState.isNull("channel_id")) {
                continue;
            }
            VoiceChannel channel = getVoiceChannelById(voiceState.getString("channel_id"));
            if (channel == null) {
                continue;
            }
            ((ImplVoiceChannel) channel).addConnectedUser(user);
            ((ImplUser) user).setVoiceChannel(channel);
        }

        JSONArray presences = new JSONArray();
        if (data.has("presences")) {
            presences = data.getJSONArray("presences");
        }
        for (int i = 0; i < presences.length(); i++) {
            JSONObject presence = presences.getJSONObject(i);
            User user = api.getOrCreateUser(presence.getJSONObject("user"));
            if (user != null && presence.has("game") && !presence.isNull("game")) {
                if (presence.getJSONObject("game").has("name") && !presence.getJSONObject("game").isNull("name")) {
                    ((ImplUser) user).setGame(presence.getJSONObject("game").getString("name"));
                }
            }
            if (user != null && presence.has("status") && !presence.isNull("status")) {
                UserStatus status = UserStatus.fromString(presence.getString("status"));
                ((ImplUser) user).setStatus(status);
            }
        }
        
        this.iconHash = data.isNull("icon") ? null : data.getString("icon");

        api.getServerMap().put(id, this);
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
    public Future<Void> delete() {
        return api.getThreadPool().getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Trying to delete server {}", ImplServer.this);
                HttpResponse<JsonNode> response = Unirest.delete("https://discordapp.com/api/v6/guilds/" + id)
                        .header("authorization", api.getToken())
                        .asJson();
                api.checkResponse(response);
                api.checkRateLimit(response, RateLimitType.UNKNOWN, ImplServer.this, null);
                api.getServerMap().remove(id);
                logger.info("Deleted server {}", ImplServer.this);
                api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                    @Override
                    public void run() {
                        List<ServerLeaveListener> listeners = api.getListeners(ServerLeaveListener.class);
                        synchronized (listeners) {
                            for (ServerLeaveListener listener : listeners) {
                                try {
                                    listener.onServerLeave(api, ImplServer.this);
                                } catch (Exception t) {
                                    logger.warn("Uncaught exception in ServerLeaveListener!", t);
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
    public Future<Void> leave() {
        return api.getThreadPool().getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Trying to leave server {}", ImplServer.this);
                HttpResponse<JsonNode> response = Unirest
                        .delete("https://discordapp.com/api/v6/users/@me/guilds/" + id)
                        .header("authorization", api.getToken())
                        .asJson();
                api.checkResponse(response);
                api.checkRateLimit(response, RateLimitType.UNKNOWN, ImplServer.this, null);
                api.getServerMap().remove(id);
                logger.info("Left server {}", ImplServer.this);
                api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                    @Override
                    public void run() {
                        List<ServerLeaveListener> listeners = api.getListeners(ServerLeaveListener.class);
                        synchronized (listeners) {
                            for (ServerLeaveListener listener : listeners) {
                                try {
                                    listener.onServerLeave(api, ImplServer.this);
                                } catch (Exception t) {
                                    logger.warn("Uncaught exception in ServerLeaveListener!", t);
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
    public Channel getChannelById(String id) {
        return channels.get(id);
    }

    @Override
    public Collection<Channel> getChannels() {
        return Collections.unmodifiableCollection(channels.values());
    }

    @Override
    public VoiceChannel getVoiceChannelById(String id) {
        return voiceChannels.get(id);
    }

    @Override
    public Collection<VoiceChannel> getVoiceChannels() {
        return Collections.unmodifiableCollection(voiceChannels.values());
    }

    @Override
    public User getMemberById(String id) {
        return members.get(id);
    }

    @Override
    public Collection<User> getMembers() {
        return Collections.unmodifiableCollection(members.values());
    }

    @Override
    public boolean isMember(User user) {
        return isMember(user.getId());
    }

    @Override
    public boolean isMember(String userId) {
        return members.containsKey(userId);
    }

    @Override
    public Collection<Role> getRoles() {
        return Collections.unmodifiableCollection(roles.values());
    }

    @Override
    public Role getRoleById(String id) {
        return roles.get(id);
    }

    @Override
    public Future<Channel> createChannel(String name) {
        return createChannel(name, null);
    }

    @Override
    public Future<Channel> createChannel(final String name, FutureCallback<Channel> callback) {
        ListenableFuture<Channel> future =
                api.getThreadPool().getListeningExecutorService().submit(new Callable<Channel>() {
                    @Override
                    public Channel call() throws Exception {
                        final Channel channel = (Channel) createChannelBlocking(name, false);
                        logger.info("Created channel in server {} (name: {}, voice: {}, id: {})",
                                ImplServer.this, channel.getName(), false, channel.getId());
                        api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                            @Override
                            public void run() {
                                List<ChannelCreateListener> listeners = api.getListeners(ChannelCreateListener.class);
                                synchronized (listeners) {
                                    for (ChannelCreateListener listener : listeners) {
                                        try {
                                            listener.onChannelCreate(api, channel);
                                        } catch (Exception t) {
                                            logger.warn("Uncaught exception in ChannelCreateListener!", t);
                                        }
                                    }
                                }
                            }
                        });
                        return channel;
                    }
                });
        if (callback != null) {
            Futures.addCallback(future, callback);
        }
        return future;
    }

    @Override
    public Future<VoiceChannel> createVoiceChannel(String name) {
        return createVoiceChannel(name, null);
    }

    @Override
    public Future<VoiceChannel> createVoiceChannel(final String name, FutureCallback<VoiceChannel> callback) {
        ListenableFuture<VoiceChannel> future =
                api.getThreadPool().getListeningExecutorService().submit(new Callable<VoiceChannel>() {
                    @Override
                    public VoiceChannel call() throws Exception {
                        final VoiceChannel channel = (VoiceChannel) createChannelBlocking(name, true);
                        logger.info("Created channel in server {} (name: {}, voice: {}, id: {})",
                                ImplServer.this, channel.getName(), true, channel.getId());
                        api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                            @Override
                            public void run() {
                                List<VoiceChannelCreateListener> listeners =
                                        api.getListeners(VoiceChannelCreateListener.class);
                                synchronized (listeners) {
                                    for (VoiceChannelCreateListener listener : listeners) {
                                        try {
                                        listener.onVoiceChannelCreate(api, channel);
                                        } catch (Exception t) {
                                            logger.warn("Uncaught exception in VoiceChannelCreateListener!", t);
                                        }
                                    }
                                }
                            }
                        });
                        return channel;
                    }
                });
        if (callback != null) {
            Futures.addCallback(future, callback);
        }
        return future;
    }

    @Override
    public Future<Invite[]> getInvites() {
        return getInvites(null);
    }

    @Override
    public Future<Invite[]> getInvites(FutureCallback<Invite[]> callback) {
        ListenableFuture<Invite[]> future = api.getThreadPool().getListeningExecutorService().submit(
                new Callable<Invite[]>() {
                    @Override
                    public Invite[] call() throws Exception {
                        logger.debug("Trying to get invites for server {}", ImplServer.this);
                        HttpResponse<JsonNode> response = Unirest
                                .get("https://discordapp.com/api/v6/guilds/" + getId() + "/invites")
                                .header("authorization", api.getToken())
                                .asJson();
                        api.checkResponse(response);
                        api.checkRateLimit(response, RateLimitType.UNKNOWN, ImplServer.this, null);
                        Invite[] invites = new Invite[response.getBody().getArray().length()];
                        for (int i = 0; i < response.getBody().getArray().length(); i++) {
                            invites[i] = new ImplInvite(api, response.getBody().getArray().getJSONObject(i));
                        }
                        logger.debug("Got invites for server {} (amount: {})", ImplServer.this, invites.length);
                        return invites;
                    }
                });
        if (callback != null) {
            Futures.addCallback(future, callback);
        }
        return future;
    }

    @Override
    public Future<Void> updateRoles(final User user, final Role[] roles) {
        final String[] roleIds = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            roleIds[i] = roles[i].getId();
        }
        return api.getThreadPool().getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Trying to update roles in server {} (amount: {})", ImplServer.this, roles.length);
                HttpResponse<JsonNode> response = Unirest
                        .patch("https://discordapp.com/api/v6/guilds/" + getId() + "/members/" + user.getId())
                        .header("authorization", api.getToken())
                        .header("Content-Type", "application/json")
                        .body(new JSONObject().put("roles", roleIds).toString())
                        .asJson();
                api.checkResponse(response);
                api.checkRateLimit(response, RateLimitType.UNKNOWN, ImplServer.this, null);
                for (final Role role : user.getRoles(ImplServer.this)) {
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
                                List<UserRoleRemoveListener> listeners =
                                        api.getListeners(UserRoleRemoveListener.class);
                                synchronized (listeners) {
                                    for (UserRoleRemoveListener listener : listeners) {
                                        try {
                                            listener.onUserRoleRemove(api, user, role);
                                        } catch (Exception t) {
                                            logger.warn("Uncaught exception in UserRoleRemoveListener!", t);
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
                for (final Role role : roles) {
                    if (!user.getRoles(ImplServer.this).contains(role)) {
                        ((ImplRole) role).addUserNoUpdate(user);
                        api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                            @Override
                            public void run() {
                                List<UserRoleAddListener> listeners = api.getListeners(UserRoleAddListener.class);
                                synchronized (listeners) {
                                    for (UserRoleAddListener listener : listeners) {
                                        try {
                                            listener.onUserRoleAdd(api, user, role);
                                        } catch (Exception t) {
                                            logger.warn("Uncaught exception in UserRoleAddListener!", t);
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
                logger.debug("Updated roles in server {} (amount: {})", ImplServer.this, getRoles().size());
                return null;
            }
        });
    }

    @Override
    public Future<Void> banUser(User user) {
        return banUser(user.getId(), 0);
    }

    @Override
    public Future<Void> banUser(String userId) {
        return banUser(userId, 0);
    }

    @Override
    public Future<Void> banUser(User user, int deleteDays) {
        return banUser(user.getId(), deleteDays);
    }

    @Override
    public Future<Void> banUser(final String userId, final int deleteDays) {
        return api.getThreadPool().getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Trying to ban an user from server {} (user id: {}, delete days: {})",
                        ImplServer.this, userId, deleteDays);
                HttpResponse<JsonNode> response = Unirest
                        .put("https://discordapp.com/api/v6/guilds/" + getId() + "/bans/" + userId
                                + "?delete-message-days=" + deleteDays)
                        .header("authorization", api.getToken())
                        .asJson();
                api.checkResponse(response);
                api.checkRateLimit(response, RateLimitType.UNKNOWN, ImplServer.this, null);
                final User user = api.getUserById(userId).get();
                if (user != null) {
                    removeMember(user);
                }
                logger.info("Banned an user from server {} (user id: {}, delete days: {})",
                        ImplServer.this, userId, deleteDays);
                api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                    @Override
                    public void run() {
                        List<ServerMemberBanListener> listeners = api.getListeners(ServerMemberBanListener.class);
                        synchronized (listeners) {
                            for (ServerMemberBanListener listener : listeners) {
                                try {
                                    listener.onServerMemberBan(api, user, ImplServer.this);
                                } catch (Exception t) {
                                    logger.warn("Uncaught exception in ServerMemberBanListener!", t);
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
    public Future<Void> unbanUser(final String userId) {
        return api.getThreadPool().getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Trying to unban an user from server {} (user id: {})", ImplServer.this, userId);
                HttpResponse<JsonNode> response = Unirest
                        .delete("https://discordapp.com/api/v6/guilds/" + getId() + "/bans/" + userId)
                        .header("authorization", api.getToken())
                        .asJson();
                api.checkResponse(response);
                api.checkRateLimit(response, RateLimitType.UNKNOWN, ImplServer.this, null);
                logger.info("Unbanned an user from server {} (user id: {})", ImplServer.this, userId);
                api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                    @Override
                    public void run() {
                        List<ServerMemberUnbanListener> listeners =
                                api.getListeners(ServerMemberUnbanListener.class);
                        synchronized (listeners) {
                            for (ServerMemberUnbanListener listener : listeners) {
                                try {
                                    listener.onServerMemberUnban(api, userId, ImplServer.this);
                                } catch (Exception t) {
                                    logger.warn("Uncaught exception in ServerMemberUnbanListener!", t);
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
    public Future<Ban[]> getBans() {
        return getBans(null);
    }

    @Override
    public Future<Ban[]> getBans(FutureCallback<Ban[]> callback) {
        ListenableFuture<Ban[]> future =
                api.getThreadPool().getListeningExecutorService().submit(new Callable<Ban[]>() {
                    @Override
                    public Ban[] call() throws Exception {
                        logger.debug("Trying to get bans for server {}", ImplServer.this);
                        HttpResponse<JsonNode> response = Unirest
                                .get("https://discordapp.com/api/v6/guilds/" + getId() + "/bans")
                                .header("authorization", api.getToken())
                                .asJson();
                        api.checkResponse(response);
                        api.checkRateLimit(response, RateLimitType.UNKNOWN, ImplServer.this, null);
                        JSONArray bansJson = response.getBody().getArray();
                        Ban[] bans = new Ban[bansJson.length()];
                        for (int i = 0; i < bansJson.length(); i++) {
                            bans[i] = new ImplBan(api, ImplServer.this, bansJson.getJSONObject(i));
                        }
                        logger.debug("Got bans for server {} (amount: {})", ImplServer.this, bans.length);
                        return bans;
                    }
                });
        if (callback != null) {
            Futures.addCallback(future, callback);
        }
        return future;
    }

    @Override
    public Future<Void> kickUser(User user) {
        return kickUser(user.getId());
    }

    @Override
    public Future<Void> kickUser(final String userId) {
        return api.getThreadPool().getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Trying to kick an user from server {} (user id: {})", ImplServer.this);
                HttpResponse<JsonNode> response = Unirest
                        .delete("https://discordapp.com/api/v6/guilds/"+ getId() + "/members/" + userId)
                        .header("authorization", api.getToken())
                        .asJson();
                api.checkResponse(response);
                api.checkRateLimit(response, RateLimitType.UNKNOWN, ImplServer.this, null);
                final User user = api.getUserById(userId).get();
                if (user != null) {
                    removeMember(user);
                }
                logger.info("Kicked an user from server {} (user id: {})", ImplServer.this);
                api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                    @Override
                    public void run() {
                        List<ServerMemberRemoveListener> listeners =
                                api.getListeners(ServerMemberRemoveListener.class);
                        synchronized (listeners) {
                            for (ServerMemberRemoveListener listener : listeners) {
                                try {
                                    listener.onServerMemberRemove(api, user, ImplServer.this);
                                } catch (Exception t) {
                                    logger.warn("Uncaught exception in ServerMemberRemoveListener!", t);
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
    public Future<Role> createRole() {
        return createRole(null);
    }

    @Override
    public Future<Role> createRole(FutureCallback<Role> callback) {
        ListenableFuture<Role> future = api.getThreadPool().getListeningExecutorService().submit(new Callable<Role>() {
            @Override
            public Role call() throws Exception {
                logger.debug("Trying to create a role in server {}", ImplServer.this);
                HttpResponse<JsonNode> response = Unirest
                        .post("https://discordapp.com/api/v6/guilds/" + getId() + "/roles")
                        .header("authorization", api.getToken())
                        .asJson();
                api.checkResponse(response);
                api.checkRateLimit(response, RateLimitType.UNKNOWN, ImplServer.this, null);
                final Role role = new ImplRole(response.getBody().getObject(), ImplServer.this, api);
                logger.info("Created role in server {} (name: {}, id: {})",
                        ImplServer.this, role.getName(), role.getId());
                api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                    @Override
                    public void run() {
                        List<RoleCreateListener> listeners = api.getListeners(RoleCreateListener.class);
                        synchronized (listeners) {
                            for (RoleCreateListener listener : listeners) {
                                try {
                                    listener.onRoleCreate(api, role);
                                } catch (Exception t) {
                                    logger.warn("Uncaught exception in RoleCreateListener!", t);
                                }
                            }
                        }
                    }
                });
                return role;
            }
        });
        if (callback != null) {
            Futures.addCallback(future, callback);
        }
        return future;
    }

    @Override
    public Future<Void> updateName(String newName) {
        return update(newName, null, null);
    }

    @Override
    public Future<Void> updateRegion(Region newRegion) {
        return update(null, newRegion, null);
    }

    @Override
    public Future<Void> updateIcon(BufferedImage newIcon) {
        return update(null, null, newIcon);
    }

    @Override
    public Future<Void> update(final String newName, final Region newRegion, BufferedImage newIcon) {
        final JSONObject params = new JSONObject();
        if (newName == null) {
            params.put("name", getName());
        } else {
            params.put("name", newName);
        }
        if (newRegion != null) {
            params.put("region", newRegion.getKey());
        }

        return api.getThreadPool().getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug(
                        "Trying to update server {} (new name: {}, old name: {}, new region: {}, old region: {}",
                        ImplServer.this, newName, getName(), newRegion == null ? "null" : newRegion.getKey(),
                        getRegion().getKey());
                HttpResponse<JsonNode> response = Unirest
                        .patch("https://discordapp.com/api/v6/guilds/" + getId())
                        .header("authorization", api.getToken())
                        .header("Content-Type", "application/json")
                        .body(params.toString())
                        .asJson();
                api.checkResponse(response);
                api.checkRateLimit(response, RateLimitType.UNKNOWN, ImplServer.this, null);
                logger.debug("Updated server {} (new name: {}, old name: {}, new region: {}, old region: {}",
                        ImplServer.this, newName, getName(), newRegion == null ? "null" : newRegion.getKey(),
                        getRegion().getKey());

                String name = response.getBody().getObject().getString("name");
                if (!getName().equals(name)) {
                    final String oldName = getName();
                    api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                        @Override
                        public void run() {
                            List<ServerChangeNameListener> listeners =
                                    api.getListeners(ServerChangeNameListener.class);
                            synchronized (listeners) {
                                for (ServerChangeNameListener listener : listeners) {
                                    try {
                                        listener.onServerChangeName(api, ImplServer.this, oldName);
                                    } catch (Exception t) {
                                        logger.warn("Uncaught exception in ServerChangeNameListener!", t);
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
    public Region getRegion() {
        return region;
    }

    @Override
    public int getMemberCount() {
        return memberCount;
    }

    @Override
    public boolean isLarge() {
        return large;
    }

    @Override
    public String getOwnerId() {
        return ownerId;
    }

    @Override
    public Future<User> getOwner() {
        return api.getUserById(ownerId);
    }

    @Override
    public Collection<CustomEmoji> getCustomEmojis() {
        return customEmojis.values();
    }

    @Override
    public CustomEmoji getCustomEmojiById(String id) {
        return customEmojis.get(id);
    }

    @Override
    public CustomEmoji getCustomEmojiByName(String name) {
        for (CustomEmoji emoji : customEmojis.values()) {
            if (emoji.getName().equals(name)) {
                return emoji;
            }
        }
        return null;
    }

    @Override
    public String getNickname(User user) {
        return nicknames.get(user.getId());
    }

    @Override
    public boolean hasNickname(User user) {
        return nicknames.contains(user.getId());
    }

    @Override
    public Future<Void> updateNickname(final User user, final String nickname) {
        return api.getThreadPool().getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Trying to update nickname of user {} to {}", user, nickname);
                String url = "https://discordapp.com/api/v6/guilds/" + getId() + "/members/" + user.getId();
                if (user.isYourself()) {
                    url = "https://discordapp.com/api/v6/guilds/" + getId() + "/members/@me/nick";
                }
                HttpResponse<JsonNode> response = Unirest
                        .patch(url)
                        .header("authorization", api.getToken())
                        .header("Content-Type", "application/json")
                        .body(new JSONObject()
                                .put("nick", nickname)
                                .toString())
                        .asJson();
                api.checkResponse(response);
                api.checkRateLimit(response, RateLimitType.UNKNOWN, ImplServer.this, null);
                logger.debug("Updated nickname of user {} to {}", user, nickname);
                return null;
            }
        });
    }

    @Override
    public URL getIconUrl() {
        if (iconHash == null) {
            return null;
        }
        try {
            return new URL("https://cdn.discordapp.com/icons/" + id + "/" + iconHash + ".png");
        } catch (MalformedURLException e) {
            logger.warn("Seems like the url of the icon is malformed! Please contact the developer!", e);
            return null;
        }
    }

    public Future<byte[]> getIconAsByteArray() {
        ListenableFuture<byte[]> future =
                api.getThreadPool().getListeningExecutorService().submit(new Callable<byte[]>() {
                    @Override
                    public byte[] call() throws Exception {
                        logger.debug("Trying to get icon from server {}", ImplServer.this);
                        if (iconHash == null) {
                            logger.debug("Server {} has default icon. Returning empty array!", ImplServer.this);
                            return new byte[0];
                        }
                        URL url = getIconUrl();
                        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                        conn.setRequestProperty("User-Agent", Javacord.USER_AGENT);
                        InputStream in = new BufferedInputStream(conn.getInputStream());
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        byte[] buf = new byte[1024];
                        int n;
                        while (-1 != (n = in.read(buf))) {
                            out.write(buf, 0, n);
                        }
                        out.close();
                        in.close();
                        byte[] avatar = out.toByteArray();
                        logger.debug("Got icon from server {} (size: {})", ImplServer.this, avatar.length);
                        return avatar;
                    }
                });
        return future;
    }

    @Override
    public Future<byte[]> getIcon() {
        return getIcon(null);
    }

    @Override
    public Future<byte[]> getIcon(FutureCallback<byte[]> callback) {
        ListenableFuture<byte[]> future =
                api.getThreadPool().getListeningExecutorService().submit(new Callable<byte[]>() {
                    @Override
                    public byte[] call() throws Exception {
                        byte[] imageAsBytes = getIconAsByteArray().get();
                        if (imageAsBytes.length == 0) {
                            return null;
                        }
                        return imageAsBytes;
                    }
                });
        if (callback != null) {
            Futures.addCallback(future, callback);
        }
        return future;
    }

    /**
     * Sets the name of the server.
     *
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the region of the server.
     *
     * @param region The region to set.
     */
    public void setRegion(Region region) {
        this.region = region;
    }

    /**
     * Adds a user to the server.
     *
     * @param user The user to add.
     */
    public void addMember(User user) {
        members.put(user.getId(), user);
    }

    /**
     * Adds members to the server.
     *
     * @param members An array of guild member objects.
     */
    public void addMembers(JSONArray members) {
        for (int i = 0; i < members.length(); i++) {
            User member = api.getOrCreateUser(members.getJSONObject(i).getJSONObject("user"));
            if (members.getJSONObject(i).has("nick") && !members.getJSONObject(i).isNull("nick")) {
                nicknames.put(member.getId(), members.getJSONObject(i).getString("nick"));
            }
            this.members.put(member.getId(), member);

            JSONArray memberRoles = members.getJSONObject(i).getJSONArray("roles");
            for (int j = 0; j < memberRoles.length(); j++) {
                Role role = getRoleById(memberRoles.getString(j));
                if (role != null) {
                    ((ImplRole) role).addUserNoUpdate(member);
                }
            }
        }
    }

    /**
     * Removes a user from the server.
     *
     * @param user The user to remove.
     */
    public void removeMember(User user) {
        members.remove(user.getId());
        for (Role role : getRoles()) {
            ((ImplRole) role).removeUserNoUpdate(user);
        }
        for (Channel channel : getChannels()) {
            ((ImplChannel) channel).removeOverwrittenPermissions(user);
        }
        for (VoiceChannel channel : getVoiceChannels()) {
            ((ImplVoiceChannel) channel).removeOverwrittenPermissions(user);
        }
    }

    /**
     * Increments the member count.
     */
    public void incrementMemberCount() {
        memberCount++;
    }

    /**
     * Decrement the member count.
     */
    public void decrementMemberCount() {
        memberCount--;
    }

    /**
     * Sets the member count.
     *
     * @param memberCount The member count to set.
     */
    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    /**
     * Adds a channel to the server.
     *
     * @param channel The channel to add.
     */
    public void addChannel(Channel channel) {
        channels.put(channel.getId(), channel);
    }

    /**
     * Adds a voice channel to the server.
     *
     * @param channel The voice channel to add.
     */
    public void addVoiceChannel(VoiceChannel channel) {
        voiceChannels.put(channel.getId(), channel);
    }

    /**
     * Adds a role to the server.
     *
     * @param role The role to add.
     */
    public void addRole(Role role) {
        roles.put(role.getId(), role);
    }

    /**
     * Removes a role from the server.
     *
     * @param role The role to remove.
     */
    public void removeRole(Role role) {
        roles.remove(role.getId());
    }

    /**
     * Removes a channel from the server.
     *
     * @param channel The channel to remove.
     */
    public void removeChannel(Channel channel) {
        channels.remove(channel.getId());
    }

    /**
     * Removes a voice channel from the server.
     *
     * @param channel The voice channel to remove.
     */
    public void removeVoiceChannel(VoiceChannel channel) {
        voiceChannels.remove(channel.getId());
    }

    /**
     * Adds a emoji to the server.
     *
     * @param emoji The emoji to add.
     */
    public void addCustomEmoji(CustomEmoji emoji) {
        customEmojis.put(emoji.getId(), emoji);
    }

    /**
     * Removes a emoji from the server.
     *
     * @param emoji The emoji to remove.
     */
    public void removeCustomEmoji(CustomEmoji emoji) {
        customEmojis.remove(emoji.getId());
    }

    /**
     * Sets the owner id.
     *
     * @param ownerId The id of the owner.
     */
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Sets the nickname of an user.
     *
     * @param user The user.
     * @param nickname The nickname to set.
     */
    public void setNickname(User user, String nickname) {
        if (nickname == null) {
            nicknames.remove(user.getId());
        } else {
            nicknames.put(user.getId(), nickname);
        }
    }

    /**
     * Gets the icon hash of the server.
     *
     * @return The icon hash of the server.
     */
    public String getIconHash() {
        return this.iconHash;
    }

    /**
     * Sets the icon hash of the server.
     *
     * @param hash The hash to use.
     */
    public void setIconHash(String hash) {
        this.iconHash = hash;
    }

    /**
     * Creates a new channel.
     *
     * @param name The name of the channel.
     * @param voice Whether the channel should be voice or text.
     * @return The created channel.
     * @throws Exception If something went wrong.
     */
    private Object createChannelBlocking(String name, boolean voice) throws Exception {
        logger.debug("Trying to create channel in server {} (name: {}, voice: {})", ImplServer.this, name, voice);
        JSONObject param = new JSONObject().put("name", name).put("type", voice ? "voice" : "text");
        HttpResponse<JsonNode> response = Unirest.post("https://discordapp.com/api/v6/guilds/" + id + "/channels")
                .header("authorization", api.getToken())
                .header("Content-Type", "application/json")
                .body(param.toString())
                .asJson();
        api.checkResponse(response);
        api.checkRateLimit(response, RateLimitType.UNKNOWN, ImplServer.this, null);
        if (voice) {
            return new ImplVoiceChannel(response.getBody().getObject(), this, api);
        } else {
            return new ImplChannel(response.getBody().getObject(), this, api);
        }
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
