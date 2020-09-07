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

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.body.MultipartBody;
import com.zacharytalis.alttextbot.ImplDiscordAPI;
import com.zacharytalis.alttextbot.entities.Channel;
import com.zacharytalis.alttextbot.entities.InviteBuilder;
import com.zacharytalis.alttextbot.entities.Server;
import com.zacharytalis.alttextbot.entities.User;
import com.zacharytalis.alttextbot.entities.message.Message;
import com.zacharytalis.alttextbot.entities.message.MessageHistory;
import com.zacharytalis.alttextbot.entities.message.MessageReceiver;
import com.zacharytalis.alttextbot.entities.message.embed.EmbedBuilder;
import com.zacharytalis.alttextbot.entities.message.impl.ImplMessage;
import com.zacharytalis.alttextbot.entities.message.impl.ImplMessageHistory;
import com.zacharytalis.alttextbot.entities.permissions.Permissions;
import com.zacharytalis.alttextbot.entities.permissions.Role;
import com.zacharytalis.alttextbot.entities.permissions.impl.ImplPermissions;
import com.zacharytalis.alttextbot.entities.permissions.impl.ImplRole;
import com.zacharytalis.alttextbot.listener.channel.ChannelChangeNameListener;
import com.zacharytalis.alttextbot.listener.channel.ChannelChangePositionListener;
import com.zacharytalis.alttextbot.listener.channel.ChannelChangeTopicListener;
import com.zacharytalis.alttextbot.listener.channel.ChannelDeleteListener;
import com.zacharytalis.alttextbot.utils.LoggerUtil;
import com.zacharytalis.alttextbot.utils.SnowflakeUtil;
import com.zacharytalis.alttextbot.utils.ratelimits.RateLimitType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * The implementation of the channel interface.
 */
public class ImplChannel implements Channel {

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerUtil.getLogger(ImplChannel.class);

    private static final Permissions emptyPermissions = new ImplPermissions(0, 0);

    private final ImplDiscordAPI api;

    private final String id;
    private String name;
    private String topic = null;
    private String parentId = null;
    private int position;
    private final ImplServer server;

    private final ConcurrentHashMap<String, Permissions> overwrittenPermissions = new ConcurrentHashMap<>();

    /**
     * Creates a new instance of this class.
     *
     * @param data A JSONObject containing all necessary data.
     * @param server The server of the channel.
     * @param api The api of this server.
     */
    public ImplChannel(JSONObject data, ImplServer server, ImplDiscordAPI api) {
        this.api = api;
        this.server = server;

        id = data.getString("id");
        name = data.getString("name");
        try {
            topic = data.getString("topic");
        } catch (JSONException ignored) { }
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

        server.addChannel(this);
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
    public String getTopic() {
        return topic;
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
                logger.debug("Trying to delete channel {}", ImplChannel.this);
                HttpResponse<JsonNode> response = Unirest
                        .delete("https://discordapp.com/api/v6/channels/" + id)
                        .header("authorization", api.getToken())
                        .asJson();
                api.checkResponse(response);
                server.removeChannel(ImplChannel.this);
                logger.info("Deleted channel {}", ImplChannel.this);
                // call listener
                api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                    @Override
                    public void run() {
                        List<ChannelDeleteListener> listeners = api.getListeners(ChannelDeleteListener.class);
                        synchronized (listeners) {
                            for (ChannelDeleteListener listener : listeners) {
                                listener.onChannelDelete(api, ImplChannel.this);
                            }
                        }
                    }
                });
                return null;
            }
        });
    }

    @Override
    public void type() {
        try {
            logger.debug("Sending typing state in channel {}", this);
            Unirest.post("https://discordapp.com/api/v6/channels/" + id + "/typing")
                    .header("authorization", api.getToken())
                    .asJson();
            logger.debug("Sent typing state in channel {}", this);
        } catch (UnirestException e) {
            logger.warn("Couldn't send typing state in channel {}. Please contact the developer!", this, e);
        }
    }

    @Override
    public InviteBuilder getInviteBuilder() {
        return new ImplInviteBuilder(this, api);
    }

    @Override
    public Future<Message> sendMessage(String content) {
        return sendMessage(content, null, false, null, null);
    }

    @Override
    public Future<Message> sendMessage(String content, String nonce) {
        return sendMessage(content, null, false, nonce, null);
    }

    @Override
    public Future<Message> sendMessage(String content, boolean tts) {
        return sendMessage(content, null, tts, null, null);
    }

    @Override
    public Future<Message> sendMessage(String content, boolean tts, String nonce) {
        return sendMessage(content, null, tts, nonce, null);
    }

    @Override
    public Future<Message> sendMessage(String content, EmbedBuilder embed) {
        return sendMessage(content, embed, false, null, null);
    }

    @Override
    public Future<Message> sendMessage(String content, EmbedBuilder embed, String nonce) {
        return sendMessage(content, embed, false, nonce, null);
    }

    @Override
    public Future<Message> sendMessage(String content, EmbedBuilder embed, boolean tts) {
        return sendMessage(content, embed, tts, null, null);
    }

    @Override
    public Future<Message> sendMessage(String content, EmbedBuilder embed, boolean tts, String nonce) {
        return sendMessage(content, embed, tts, nonce, null);
    }

    @Override
    public Future<Message> sendMessage(String content, FutureCallback<Message> callback) {
        return sendMessage(content, null, false, null, callback);
    }

    @Override
    public Future<Message> sendMessage(String content, String nonce, FutureCallback<Message> callback) {
        return sendMessage(content, null, false, nonce, callback);
    }

    @Override
    public Future<Message> sendMessage(String content, boolean tts, FutureCallback<Message> callback) {
        return sendMessage(content, null, tts, null, callback);
    }

    @Override
    public Future<Message> sendMessage(String content, boolean tts, String nonce, FutureCallback<Message> callback) {
        return sendMessage(content, null, tts, nonce, callback);
    }

    @Override
    public Future<Message> sendMessage(String content, EmbedBuilder embed, FutureCallback<Message> callback) {
        return sendMessage(content, embed, false, null, callback);
    }

    @Override
    public Future<Message> sendMessage(String content, EmbedBuilder embed, String nonce, FutureCallback<Message> callback) {
        return sendMessage(content, embed, false, nonce, callback);
    }

    @Override
    public Future<Message> sendMessage(String content, EmbedBuilder embed, boolean tts, FutureCallback<Message> callback) {
        return sendMessage(content, embed, tts, null, callback);
    }

    @Override
    public Future<Message> sendMessage(final String content, final EmbedBuilder embed, final boolean tts, final String nonce, FutureCallback<Message> callback) {
        final MessageReceiver receiver = this;
        ListenableFuture<Message> future =
                api.getThreadPool().getListeningExecutorService().submit(new Callable<Message>() {
                    @Override
                    public Message call() throws Exception {
                        logger.debug("Trying to send message in channel {} (tts: {})",
                                ImplChannel.this, tts);
                        api.checkRateLimit(null, RateLimitType.SERVER_MESSAGE, null, ImplChannel.this);
                        JSONObject body = new JSONObject()
                                .put("content", content)
                                .put("tts", tts)
                                .put("mentions", new String[0]);
                        if (embed != null) {
                            body.put("embed", embed.toJSONObject());
                        }
                        if (nonce != null) {
                            body.put("nonce", nonce);
                        }
                        HttpResponse<JsonNode> response =
                                Unirest.post("https://discordapp.com/api/v6/channels/" + id + "/messages")
                                        .header("authorization", api.getToken())
                                        .header("content-type", "application/json")
                                        .body(body.toString())
                                        .asJson();
                        api.checkResponse(response);
                        api.checkRateLimit(response, RateLimitType.SERVER_MESSAGE, null, ImplChannel.this);
                        logger.debug("Sent message in channel {} (tts: {})",
                                ImplChannel.this, tts);
                        return new ImplMessage(response.getBody().getObject(), api, receiver);
                    }
                });
        if (callback != null) {
            Futures.addCallback(future, callback);
        }
        return future;
    }

    @Override
    public Future<Message> sendFile(final File file) {
        return sendFile(file, null, null);
    }

    @Override
    public Future<Message> sendFile(final File file, FutureCallback<Message> callback) {
        return sendFile(file, null, callback);
    }

    @Override
    public Future<Message> sendFile(InputStream inputStream, String filename) {
        return sendFile(inputStream, filename, null, null);
    }

    @Override
    public Future<Message> sendFile(InputStream inputStream, String filename, FutureCallback<Message> callback) {
        return sendFile(inputStream, filename, null, callback);
    }

    @Override
    public Future<Message> sendFile(File file, String comment) {
        return sendFile(file, comment, null);
    }

    @Override
    public Future<Message> sendFile(final File file, final String comment, FutureCallback<Message> callback) {
        final MessageReceiver receiver = this;
        ListenableFuture<Message> future =
                api.getThreadPool().getListeningExecutorService().submit(new Callable<Message>() {
                    @Override
                    public Message call() throws Exception {
                        logger.debug("Trying to send a file in channel {} (name: {}, comment: {})",
                                ImplChannel.this, file.getName(), comment);
                        api.checkRateLimit(null, RateLimitType.SERVER_MESSAGE, null, ImplChannel.this);
                        MultipartBody body = Unirest
                                .post("https://discordapp.com/api/v6/channels/" + id + "/messages")
                                .header("authorization", api.getToken())
                                .field("file", file);
                        if (comment != null) {
                            body.field("content", comment);
                        }
                        HttpResponse<JsonNode> response = body.asJson();
                        api.checkResponse(response);
                        api.checkRateLimit(response, RateLimitType.SERVER_MESSAGE, null, ImplChannel.this);
                        logger.debug("Sent a file in channel {} (name: {}, comment: {})",
                                ImplChannel.this, file.getName(), comment);
                        return new ImplMessage(response.getBody().getObject(), api, receiver);
                    }
                });
        if (callback != null) {
            Futures.addCallback(future, callback);
        }
        return future;
    }

    @Override
    public Future<Message> sendFile(InputStream inputStream, String filename, String comment) {
        return sendFile(inputStream, filename, comment, null);
    }

    @Override
    public Future<Message> sendFile(final InputStream inputStream, final String filename, final String comment,
                                    FutureCallback<Message> callback) {
        final MessageReceiver receiver = this;
        ListenableFuture<Message> future =
                api.getThreadPool().getListeningExecutorService().submit(new Callable<Message>() {
                    @Override
                    public Message call() throws Exception {
                        logger.debug("Trying to send an input stream in channel {} (comment: {})",
                                ImplChannel.this, comment);
                        api.checkRateLimit(null, RateLimitType.SERVER_MESSAGE, null, ImplChannel.this);
                        MultipartBody body = Unirest
                                .post("https://discordapp.com/api/v6/channels/" + id + "/messages")
                                .header("authorization", api.getToken())
                                .field("file", inputStream, filename);
                        if (comment != null) {
                            body.field("content", comment);
                        }
                        HttpResponse<JsonNode> response = body.asJson();
                        api.checkResponse(response);
                        api.checkRateLimit(response, RateLimitType.SERVER_MESSAGE, null, ImplChannel.this);
                        logger.debug("Sent an input stream in channel {} (comment: {})", ImplChannel.this, comment);
                        return new ImplMessage(response.getBody().getObject(), api, receiver);
                    }
                });
        if (callback != null) {
            Futures.addCallback(future, callback);
        }
        return future;
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
    public Future<MessageHistory> getMessageHistory(int limit) {
        return getMessageHistory(null, false, limit, null);
    }

    @Override
    public Future<MessageHistory> getMessageHistory(int limit, FutureCallback<MessageHistory> callback) {
        return getMessageHistory(null, false, limit, callback);
    }

    @Override
    public Future<MessageHistory> getMessageHistoryBefore(Message before, int limit) {
        return getMessageHistory(before.getId(), true, limit, null);
    }

    @Override
    public Future<MessageHistory> getMessageHistoryBefore(
            Message before, int limit, FutureCallback<MessageHistory> callback) {
        return getMessageHistory(before.getId(), true, limit, callback);
    }

    @Override
    public Future<MessageHistory> getMessageHistoryBefore(String beforeId, int limit) {
        return getMessageHistory(beforeId, true, limit, null);
    }

    @Override
    public Future<MessageHistory> getMessageHistoryBefore(
            String beforeId, int limit, FutureCallback<MessageHistory> callback) {
        return getMessageHistory(beforeId, true, limit, callback);
    }

    @Override
    public Future<MessageHistory> getMessageHistoryAfter(Message after, int limit) {
        return getMessageHistory(after.getId(), false, limit, null);
    }

    @Override
    public Future<MessageHistory> getMessageHistoryAfter(
            Message after, int limit, FutureCallback<MessageHistory> callback) {
        return getMessageHistory(after.getId(), false, limit, callback);
    }

    @Override
    public Future<MessageHistory> getMessageHistoryAfter(String afterId, int limit) {
        return getMessageHistory(afterId, false, limit, null);
    }

    @Override
    public Future<MessageHistory> getMessageHistoryAfter(
            String afterId, int limit, FutureCallback<MessageHistory> callback) {
        return getMessageHistory(afterId, false, limit, callback);
    }

    @Override
    public Future<Void> updateName(String newName) {
        return update(newName, getTopic(), getPosition());
    }

    @Override
    public Future<Void> updateTopic(String newTopic) {
        return update(getName(), newTopic, getPosition());
    }

    @Override
    public Future<Void> updatePosition(int newPosition) {
        return update(getName(), getTopic(), newPosition);
    }

    @Override
    public Future<Void> update(final String newName, final String newTopic, final int newPosition) {
        final JSONObject params = new JSONObject()
                .put("name", newName)
                .put("topic", newTopic)
                .put("position", newPosition);
        return api.getThreadPool().getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Trying to update channel {} (new name: {}, old name: {}, new topic: {}, old topic: {}, new position: {}, old position: {})",
                        ImplChannel.this, newName, getName(), newTopic, getTopic(), newPosition, getPosition());
                HttpResponse<JsonNode> response = Unirest
                        .patch("https://discordapp.com/api/v6/channels/" + getId())
                        .header("authorization", api.getToken())
                        .header("Content-Type", "application/json")
                        .body(params.toString())
                        .asJson();
                api.checkResponse(response);
                api.checkRateLimit(response, RateLimitType.UNKNOWN, server, null);
                logger.info("Updated channel {} (new name: {}, old name: {}, new topic: {}, old topic: {}, new position: {}, old position: {})",
                        ImplChannel.this, newName, getName(), newTopic, getTopic(), newPosition, getPosition());
                String updatedName = response.getBody().getObject().getString("name");
                String updatedTopic = null;
                if (response.getBody().getObject().has("topic")
                        && !response.getBody().getObject().isNull("topic")) {
                    updatedTopic = response.getBody().getObject().getString("topic");
                }
                int updatedPosition = response.getBody().getObject().getInt("position");

                // check name
                if (!updatedName.equals(getName())) {
                    final String oldName = getName();
                    setName(updatedName);
                    api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                        @Override
                        public void run() {
                            List<ChannelChangeNameListener> listeners =
                                    api.getListeners(ChannelChangeNameListener.class);
                            synchronized (listeners) {
                                for (ChannelChangeNameListener listener : listeners) {
                                    try {
                                        listener.onChannelChangeName(api, ImplChannel.this, oldName);
                                    } catch (Exception t) {
                                        logger.warn("Uncaught exception in ChannelChangeNameListener!", t);
                                    }
                                }
                            }
                        }
                    });
                }

                // check topic
                if ((getTopic() != null && updatedTopic == null) || (getTopic() == null && updatedTopic != null)
                        || (getTopic() != null && !getTopic().equals(updatedTopic))) {
                    final String oldTopic = getTopic();
                    setTopic(updatedTopic);
                    api.getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                        @Override
                        public void run() {
                            List<ChannelChangeTopicListener> listeners =
                                    api.getListeners(ChannelChangeTopicListener.class);
                            synchronized (listeners) {
                                for (ChannelChangeTopicListener listener : listeners) {
                                    try {
                                        listener.onChannelChangeTopic(api, ImplChannel.this, oldTopic);
                                    } catch (Exception t) {
                                        logger.warn("Uncaught exception in ChannelChangeTopicListener!", t);
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
                            List<ChannelChangePositionListener> listeners =
                                    api.getListeners(ChannelChangePositionListener.class);
                            synchronized (listeners) {
                                for (ChannelChangePositionListener listener : listeners) {
                                    try {
                                        listener.onChannelChangePosition(api, ImplChannel.this, oldPosition);
                                    } catch (Exception t) {
                                        logger.warn("Uncaught exception in ChannelChangePositionListener!", t);
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
    public String getMentionTag() {
        return "<#" + getId() + ">";
    }

    @Override
    public Future<Void> bulkDelete(final String... messages) {
        return api.getThreadPool().getListeningExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Bulk deleting messages in channel {} (ids: [{}])", this, Joiner.on(",").join(messages));
                api.checkRateLimit(null, RateLimitType.SERVER_MESSAGE, null, ImplChannel.this);
                HttpResponse<JsonNode> response =
                        Unirest.post("https://discordapp.com/api/v6/channels/" + getId() + "/messages/bulk-delete")
                                .header("authorization", api.getToken())
                                .header("Content-Type", "application/json")
                                .body(new JSONObject()
                                        .put("messages", messages)
                                        .toString())
                                .asJson();
                api.checkRateLimit(response, RateLimitType.SERVER_MESSAGE, null, ImplChannel.this);
                logger.debug("Bulk deleted messages in channel {} (ids: [{}])", this, Joiner.on(",").join(messages));
                return null;
            }
        });
    }

    @Override
    public Future<Void> bulkDelete(Message... messages) {
        String[] messageIds = new String[messages.length];
        for (int i = 0; i < messages.length; i++) {
            messageIds[i] = messages[i].getId();
        }
        return bulkDelete(messageIds);
    }

    @Override
    public Future<Message> getMessageById(final String messageId) {
        Message message = api.getMessageById(messageId);
        if (message != null) {
            return Futures.immediateFuture(message);
        }
        return api.getThreadPool().getListeningExecutorService().submit(new Callable<Message>() {
            @Override
            public Message call() throws Exception {
                logger.debug("Requesting message (channel id: {}, message id: {})", id, messageId);
                HttpResponse<JsonNode> response =
                        Unirest.get("https://discordapp.com/api/v6/channels/" + id + "/messages/" + messageId)
                                .header("authorization", api.getToken())
                                .asJson();
                api.checkResponse(response);
                api.checkRateLimit(response, RateLimitType.UNKNOWN, null, ImplChannel.this);
                Message message;
                // Synchronize on api to prevent two method calls causing duplicate objects.
                synchronized (api) {
                    message = api.getMessageById(messageId);
                    if (message == null) {
                        message = new ImplMessage(response.getBody().getObject(), api, ImplChannel.this);
                    }
                    logger.debug("Got message (channel id: {}, message id: {}, message: {})", id, messageId, message);
                }
                return message;
            }
        });
    }

    /**
     * Gets the message history.
     *
     * @param messageId Gets the messages before or after the message with the given id.
     * @param before Whether it should get the messages before or after the given message.
     * @param limit The maximum number of messages.
     * @param callback The callback.
     * @return The history.
     */
    private Future<MessageHistory> getMessageHistory(
            final String messageId, final boolean before, final int limit, FutureCallback<MessageHistory> callback) {
        ListenableFuture<MessageHistory> future = api.getThreadPool().getListeningExecutorService().submit(
                new Callable<MessageHistory>() {
                    @Override
                    public MessageHistory call() throws Exception {
                        MessageHistory history =
                                new ImplMessageHistory(api, id, messageId, before, limit);
                        api.addHistory(history);
                        return history;
                    }
                });
        if (callback != null) {
            Futures.addCallback(future, callback);
        }
        return future;
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
     * Sets the topic of the channel (no update!).
     *
     * @param topic The topic to set.
     */
    public void setTopic(String topic) {
        this.topic = topic;
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

    @Override
    public String toString() {
        return getName() + " (id: " + getId() + ")";
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
