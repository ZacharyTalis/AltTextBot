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
package com.zacharytalis.alttextbot;

import com.google.common.io.BaseEncoding;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.zacharytalis.alttextbot.entities.*;
import com.zacharytalis.alttextbot.entities.impl.ImplInvite;
import com.zacharytalis.alttextbot.entities.impl.ImplServer;
import com.zacharytalis.alttextbot.entities.impl.ImplUser;
import com.zacharytalis.alttextbot.entities.message.Message;
import com.zacharytalis.alttextbot.entities.message.MessageHistory;
import com.zacharytalis.alttextbot.entities.message.impl.ImplMessageHistory;
import com.zacharytalis.alttextbot.entities.permissions.Permissions;
import com.zacharytalis.alttextbot.entities.permissions.PermissionsBuilder;
import com.zacharytalis.alttextbot.entities.permissions.impl.ImplPermissionsBuilder;
import com.zacharytalis.alttextbot.entities.permissions.impl.ImplRole;
import com.zacharytalis.alttextbot.exceptions.BadResponseException;
import com.zacharytalis.alttextbot.exceptions.NotSupportedForBotsException;
import com.zacharytalis.alttextbot.exceptions.PermissionsException;
import com.zacharytalis.alttextbot.exceptions.RateLimitedException;
import com.zacharytalis.alttextbot.listener.Listener;
import com.zacharytalis.alttextbot.listener.server.ServerJoinListener;
import com.zacharytalis.alttextbot.listener.user.UserChangeNameListener;
import com.zacharytalis.alttextbot.utils.DiscordWebsocketAdapter;
import com.zacharytalis.alttextbot.utils.LoggerUtil;
import com.zacharytalis.alttextbot.utils.ThreadPool;
import com.zacharytalis.alttextbot.utils.ratelimits.RateLimitManager;
import com.zacharytalis.alttextbot.utils.ratelimits.RateLimitType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * The implementation of {@link DiscordAPI}.
 */
public class ImplDiscordAPI implements DiscordAPI {

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerUtil.getLogger(ImplDiscordAPI.class);

    private final ThreadPool pool;

    private String email = null;
    private String password = null;
    private String token = null;
    private String game = null;
    private String streamingUrl = null;
    private boolean idle = false;

    private boolean autoReconnect = true;

    private boolean waitForServersOnStartup = true;
    private boolean lazyLoading = false;

    private User you = null;

    private volatile int messageCacheSize = 200;

    private DiscordWebsocketAdapter socketAdapter = null;

    private RateLimitManager rateLimitManager = new RateLimitManager();

    private final ConcurrentHashMap<String, Server> servers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

    private final ArrayList<Message> messages = new ArrayList<>();

    private final ConcurrentHashMap<Class<?>, List<Listener>> listeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SettableFuture<Server>> waitingForListener = new ConcurrentHashMap<>();

    // let the garbage collector delete old histories
    private final Set<MessageHistory> messageHistories =
            Collections.newSetFromMap(new WeakHashMap<MessageHistory, Boolean>());

    private final Object listenerLock = new Object();
    private final ServerJoinListener listener = new ServerJoinListener() {
        @Override
        public void onServerJoin(DiscordAPI api, Server server) {
            synchronized (listenerLock) { // be sure to read the guild id before trying to check for waiting listeners
                SettableFuture<Server> future = waitingForListener.get(server.getId());
                if (future != null) {
                    logger.debug("Joined or created server {}. We were waiting for this server!", server);
                    waitingForListener.remove(server.getId());
                    future.set(server);
                }
            }
        }
    };

    // a set with all unavailable servers
    private final Set<String> unavailableServers = new HashSet<>();

    /**
     * Creates a new instance of this class.
     *
     * @param pool The used pool of the library.
     */
    public ImplDiscordAPI(ThreadPool pool) {
        this.pool = pool;
    }

    @Override
    public void connect(FutureCallback<DiscordAPI> callback) {
        final DiscordAPI api = this;
        Futures.addCallback(pool.getListeningExecutorService().submit(new Callable<DiscordAPI>() {
            @Override
            public DiscordAPI call() throws Exception {
                connectBlocking();
                return api;
            }
        }), callback);
    }

    @Override
    public void connectBlocking() {
        if (token == null || !checkTokenBlocking(token)) {
            if (email == null || password == null) {
                throw new IllegalArgumentException("No valid token provided AND missing email or password. Connecting not possible!");
            }
            token = requestTokenBlocking();
        }
        String gateway = requestGatewayBlocking();
        socketAdapter = new DiscordWebsocketAdapter(this, gateway);
        try {
            if (!socketAdapter.isReady().get()) {
                throw new IllegalStateException("Socket closed before ready packet was received!");
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.warn("Something went wrong while connecting. Please contact the developer!", e);
            throw new IllegalStateException("Could not figure out if ready or not. Please contact the developer!");
        }
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void setGame(String game) {
        setGame(game, null);
    }

    @Override
    public void setGame(String game, String streamingUrl) {
        this.game = game;
        this.streamingUrl = streamingUrl;
        try {
            if (socketAdapter != null && socketAdapter.isReady().isDone() && socketAdapter.isReady().get()) {
                socketAdapter.updateStatus();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getGame() {
        return game;
    }

    @Override
    public String getStreamingUrl() {
        return streamingUrl;
    }

    @Override
    public Server getServerById(String id) {
        return servers.get(id);
    }

    @Override
    public Collection<Server> getServers() {
        return Collections.unmodifiableCollection(servers.values());
    }

    @Override
    public Collection<Channel> getChannels() {
        Collection<Channel> channels = new ArrayList<>();
        for (Server server : getServers()) {
            channels.addAll(server.getChannels());
        }
        return Collections.unmodifiableCollection(channels);
    }

    @Override
    public Channel getChannelById(String id) {
        Iterator<Server> serverIterator = getServers().iterator();
        while (serverIterator.hasNext()) {
            Channel channel = serverIterator.next().getChannelById(id);
            if (channel != null) {
                return channel;
            }
        }
        return null;
    }

    @Override
    public Collection<VoiceChannel> getVoiceChannels() {
        Collection<VoiceChannel> channels = new ArrayList<>();
        for (Server server : getServers()) {
            channels.addAll(server.getVoiceChannels());
        }
        return Collections.unmodifiableCollection(channels);
    }

    @Override
    public VoiceChannel getVoiceChannelById(String id) {
        Iterator<Server> serverIterator = getServers().iterator();
        while (serverIterator.hasNext()) {
            VoiceChannel channel = serverIterator.next().getVoiceChannelById(id);
            if (channel != null) {
                return channel;
            }
        }
        return null;
    }

    @Override
    public Future<User> getUserById(final String id) {
        User user = users.get(id);
        if (user != null) {
            return Futures.immediateFuture(user);
        }
        return getThreadPool().getListeningExecutorService().submit(new Callable<User>() {
            @Override
            public User call() throws Exception {
                logger.debug("Trying request/find user with id {} who isn't cached", id);
                User user = null;
                Iterator<Server> serverIterator = getServers().iterator();
                while (serverIterator.hasNext()) {
                    Server server = serverIterator.next();
                    HttpResponse<JsonNode> response = Unirest
                            .get("https://discordapp.com/api/v6/guilds/" + server.getId() + "/members/" + id)
                            .header("authorization", token)
                            .asJson();
                    // user does not exist
                    if (response.getStatus() < 200 || response.getStatus() > 299) {
                        continue;
                    }
                    user = getOrCreateUser(response.getBody().getObject().getJSONObject("user"));
                    // add user to server
                    ((ImplServer) server).addMember(user);
                    // assign user roles
                    if (response.getBody().getObject().has("roles")) {
                        JSONArray roleIds = response.getBody().getObject().getJSONArray("roles");
                        for (int i = 0; i < roleIds.length(); i++) {
                            // add user to the role
                            ((ImplRole) server.getRoleById(roleIds.getString(i))).addUserNoUpdate(user);
                        }
                    }
                }
                if (user != null) {
                    logger.debug("Found user {} with id {}", user, id);
                } else {
                    logger.debug("No user with id {} was found", id);
                }
                return user;
            }
        });
    }

    @Override
    public User getCachedUserById(String id) {
        return users.get(id);
    }

    @Override
    public Collection<User> getUsers() {
        return Collections.unmodifiableCollection(users.values());
    }

    @Override
    public void registerListener(Listener listener) {
        for (Class<?> implementedInterface : TypeToken.of(listener.getClass()).getTypes().interfaces().rawTypes()) {
            if (Listener.class.isAssignableFrom(implementedInterface)) {
                List<Listener> listenersList = listeners.get(implementedInterface);
                if (listenersList == null) {
                    listenersList = new ArrayList<>();
                    listeners.put(implementedInterface, listenersList);
                }
                synchronized (listenersList) {
                    listenersList.add(listener);
                }
            }
        }
    }

    @Override
    public Message getMessageById(String id) {
        synchronized (messages) {
            for (Message message : messages) {
                if (message.getId().equals(id)) {
                    return message;
                }
            }
        }
        synchronized (messageHistories) {
            for (MessageHistory history : messageHistories) {
                history.getMessageById(id);
            }
        }
        return null;
    }

    @Override
    public ThreadPool getThreadPool() {
        return pool;
    }

    @Override
    public void setIdle(boolean idle) {
        this.idle = idle;
        try {
            if (socketAdapter != null && socketAdapter.isReady().isDone() && socketAdapter.isReady().get()) {
                socketAdapter.updateStatus();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isIdle() {
        return idle;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public void setToken(String token, boolean bot) {
        this.token = bot ? "Bot " + token : token;
    }

    @Override
    public boolean checkTokenBlocking(String token) {
        try {
            // only the last 0-9 digits of the token should be visible.
            // We don't want someone being able to login to an account by reading the logs.
            logger.debug("Checking token {}", token.replaceAll(".{10}", "**********"));
            HttpResponse<JsonNode> response = Unirest.get("https://discordapp.com/api/v6/users/@me/guilds")
                    .header("authorization", token)
                    .asJson();
            if (response.getStatus() < 200 || response.getStatus() > 299) {
                logger.debug("Checked token {} (valid: {})", token.replaceAll(".{10}", "**********"), false);
                return false;
            }
            logger.debug("Checked token {} (valid: {})", token.replaceAll(".{10}", "**********"), true);
            return true;
        } catch (UnirestException e) {
            return false;
        }
    }

    @Override
    public Future<Server> acceptInvite(String inviteCode) {
        return acceptInvite(inviteCode, null);
    }

    @Override
    public Future<Server> acceptInvite(final String inviteCode, FutureCallback<Server> callback) {
        if (getYourself().isBot()) {
            throw new NotSupportedForBotsException();
        }
        ListenableFuture<Server> future = getThreadPool().getListeningExecutorService().submit(new Callable<Server>() {
            @Override
            public Server call() throws Exception {
                logger.debug("Trying to accept invite (code: {})", inviteCode);
                final SettableFuture<Server> settableFuture;
                synchronized (listenerLock) {
                    HttpResponse<JsonNode> response = Unirest.post("https://discordapp.com/api/v6/invite/" + inviteCode)
                            .header("authorization", token)
                            .asJson();
                    checkResponse(response);
                    String guildId = response.getBody().getObject().getJSONObject("guild").getString("id");
                    if (getServerById(guildId) != null) {
                        throw new IllegalStateException("Already member of this server!");
                    }
                    logger.info("Accepted invite and waiting for listener to be called (code: {}, server id: {})"
                            , inviteCode, guildId);
                    settableFuture = SettableFuture.create();
                    waitingForListener.put(guildId, settableFuture);
                }
                return settableFuture.get();
            }
        });
        if (callback != null) {
            Futures.addCallback(future, callback);
        }
        return future;
    }

    @Override
    public Future<Server> createServer(String name) {
        return createServer(name, Region.US_WEST, null, null);
    }

    @Override
    public Future<Server> createServer(String name, FutureCallback<Server> callback) {
        return createServer(name, Region.US_WEST, null, callback);
    }

    @Override
    public Future<Server> createServer(String name, Region region) {
        return createServer(name, region, null, null);
    }

    @Override
    public Future<Server> createServer(String name, Region region, FutureCallback<Server> callback) {
        return createServer(name, region, null, callback);
    }

    @Override
    public Future<Server> createServer(String name, BufferedImage icon) {
        return createServer(name, Region.US_WEST, icon, null);
    }

    @Override
    public Future<Server> createServer(String name, BufferedImage icon, FutureCallback<Server> callback) {
        return createServer(name, Region.US_WEST, icon, callback);
    }

    @Override
    public Future<Server> createServer(String name, Region region, BufferedImage icon) {
        return createServer(name, region, icon, null);
    }

    @Override
    public Future<Server> createServer(
            final String name, final Region region, final BufferedImage icon, FutureCallback<Server> callback) {
        ListenableFuture<Server> future = getThreadPool().getListeningExecutorService().submit(new Callable<Server>() {
            @Override
            public Server call() throws Exception {
                logger.debug("Trying to create server (name: {}, region: {}, icon: {}",
                        name, region == null ? "null" : region.getKey(), icon != null);
                if (name == null || name.length() < 2 || name.length() > 100) {
                    throw new IllegalArgumentException("Name must be 2-100 characters long!");
                }
                JSONObject params = new JSONObject();
                if (icon != null) {
                    if (icon.getHeight() != 128 || icon.getWidth() != 128) {
                        throw new IllegalArgumentException("Icon must be 128*128px!");
                    }
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    ImageIO.write(icon, "jpg", os);
                    params.put("icon", "data:image/jpg;base64," + BaseEncoding.base64().encode(os.toByteArray()));
                }
                params.put("name", name);
                params.put("region", region == null ? Region.US_WEST.getKey() : region.getKey());
                final SettableFuture<Server> settableFuture;
                synchronized (listenerLock) {
                    HttpResponse<JsonNode> response = Unirest.post("https://discordapp.com/api/v6/guilds")
                            .header("authorization", token)
                            .header("Content-Type", "application/json")
                            .body(params.toString())
                            .asJson();
                    checkResponse(response);
                    String guildId = response.getBody().getObject().getString("id");
                    logger.info("Created server and waiting for listener to be called " +
                            "(name: {}, region: {}, icon: {}, server id: {})"
                            , name, region == null ? "null" : region.getKey(), icon != null, guildId);
                    settableFuture = SettableFuture.create();
                    waitingForListener.put(guildId, settableFuture);
                }
                return settableFuture.get();
            }
        });
        if (callback != null) {
            Futures.addCallback(future, callback);
        }
        return future;
    }

    @Override
    public User getYourself() {
        return you;
    }

    @Override
    public Future<Void> updateUsername(String newUsername) {
        return updateProfile(newUsername, null, null, null);
    }

    @Override
    public Future<Void> updateEmail(String newEmail) {
        return updateProfile(null, newEmail, null, null);
    }

    @Override
    public Future<Void> updatePassword(String newPassword) {
        return updateProfile(null, null, newPassword, null);
    }

    @Override
    public Future<Void> updateAvatar(BufferedImage newAvatar) {
        return updateProfile(null, null, null, newAvatar);
    }

    @Override
    public Future<Void> updateProfile(
            final String newUsername, String newEmail, final String newPassword, final BufferedImage newAvatar) {
        logger.debug("Trying to update profile (username: {}, email: {}, password: {}, change avatar: {}",
                newUsername, email, newPassword == null ? "null" : newPassword.replaceAll(".", "*"), newAvatar != null);
        String avatarString = getYourself().getAvatarId();
        if (newAvatar != null) {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(newAvatar, "jpg", os);
                avatarString = "data:image/jpg;base64," + BaseEncoding.base64().encode(os.toByteArray());
            } catch (IOException ignored) { }
        }
        final JSONObject params = new JSONObject()
                .put("username", newUsername == null ? getYourself().getName() : newUsername)
                .put("avatar", avatarString == null ? JSONObject.NULL : avatarString);
        if (email != null && password != null) { // do not exist in bot accounts
            params.put("email", newEmail == null ? email : newEmail)
                    .put("password", password);
        }
        if (newPassword != null) {
            params.put("new_password", newPassword);
        }
        return getThreadPool().getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                HttpResponse<JsonNode> response = Unirest
                        .patch("https://discordapp.com/api/v6/users/@me")
                        .header("authorization", token)
                        .header("Content-Type", "application/json")
                        .body(params.toString())
                        .asJson();
                checkResponse(response);
                logger.info("Updated profile (username: {}, email: {}, password: {}, change avatar: {}",
                        newUsername, email, newPassword == null ? "null" : newPassword.replaceAll(".", "*"),
                        newAvatar != null);
                ((ImplUser) getYourself()).setAvatarId(response.getBody().getObject().getString("avatar"));
                if (response.getBody().getObject().has("email") && !response.getBody().getObject().isNull("email")) {
                    setEmail(response.getBody().getObject().getString("email"));
                }
                setToken(response.getBody().getObject().getString("token"), token.startsWith("Bot "));
                final String oldName = getYourself().getName();
                ((ImplUser) getYourself()).setName(response.getBody().getObject().getString("username"));
                if (newPassword != null) {
                    password = newPassword;
                }

                if (!getYourself().getName().equals(oldName)) {
                    getThreadPool().getSingleThreadExecutorService("listeners").submit(new Runnable() {
                        @Override
                        public void run() {
                            List<UserChangeNameListener> listeners = getListeners(UserChangeNameListener.class);
                            synchronized (listeners) {
                                for (UserChangeNameListener listener : listeners) {
                                    listener.onUserChangeName(ImplDiscordAPI.this, getYourself(), oldName);
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
    public Future<Invite> parseInvite(String invite) {
        return parseInvite(invite, null);
    }

    @Override
    public Future<Invite> parseInvite(final String invite, FutureCallback<Invite> callback) {
        final String inviteCode = invite.replace("https://discord.gg/", "").replace("http://discord.gg/", "");
        ListenableFuture<Invite> future = getThreadPool().getListeningExecutorService().submit(new Callable<Invite>() {
            @Override
            public Invite call() throws Exception {
                logger.debug("Trying to parse invite {} (parsed code: {})", invite, inviteCode);
                HttpResponse<JsonNode> response = Unirest
                        .get("https://discordapp.com/api/v6/invite/" + inviteCode)
                        .header("authorization", token)
                        .asJson();
                checkResponse(response);
                logger.debug("Parsed invite {} (parsed code: {})", invite, inviteCode);
                return new ImplInvite(ImplDiscordAPI.this, response.getBody().getObject());
            }
        });
        if (callback != null) {
            Futures.addCallback(future, callback);
        }
        return future;
    }

    @Override
    public Future<Void> deleteInvite(final String inviteCode) {
        return getThreadPool().getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logger.debug("Trying to delete invite {}", inviteCode);
                HttpResponse<JsonNode> response = Unirest
                        .delete("https://discordapp.com/api/v6/invite/" + inviteCode)
                        .header("authorization", token)
                        .asJson();
                checkResponse(response);
                logger.info("Deleted invite {}", inviteCode);
                return null;
            }
        });
    }

    @Override
    public void setMessageCacheSize(int size) {
        this.messageCacheSize = size < 0 ? 0 : size;
        synchronized (messages) {
            while (messages.size() > messageCacheSize) {
                messages.remove(0);
            }
        }
    }

    @Override
    public int getMessageCacheSize() {
        return messageCacheSize;
    }

    @Override
    public PermissionsBuilder getPermissionsBuilder() {
        return new ImplPermissionsBuilder();
    }

    @Override
    public PermissionsBuilder getPermissionsBuilder(Permissions permissions) {
        return new ImplPermissionsBuilder(permissions);
    }

    @Override
    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    @Override
    public boolean isAutoReconnectEnabled() {
        return autoReconnect;
    }

    @Override
    public RateLimitManager getRateLimitManager() {
        return rateLimitManager;
    }

    @Override
    public void setWaitForServersOnStartup(boolean wait) {
        this.waitForServersOnStartup = wait;
    }

    @Override
    public boolean isWaitingForServersOnStartup() {
        return waitForServersOnStartup;
    }

    @Override
    public void disconnect() {
        if (socketAdapter != null) {
            socketAdapter.disconnect();
        }
    }

    @Override
    public void setReconnectRatelimit(int attempts, int seconds) {
        socketAdapter.setReconnectAttempts(attempts);
        socketAdapter.setRatelimitResetIntervalInSeconds(seconds);
    }

    @Override
    public void setLazyLoading(boolean enabled) {
        this.lazyLoading = enabled;
    }

    @Override
    public boolean isLazyLoading() {
        return lazyLoading;
    }

    /**
     * Gets a list with all unavailable servers.
     *
     * @return A list with all unavailable servers.
     */
    public Set<String> getUnavailableServers() {
        return unavailableServers;
    }

    /**
     * Sets yourself.
     *
     * @param user You.
     */
    public void setYourself(User user) {
        this.you = user;
    }

    /**
     * Gets or creates a user based on the given data.
     *
     * @param data The JSONObject containing the data.
     * @return The user.
     */
    public User getOrCreateUser(JSONObject data) {
        String id = data.getString("id");
        User user = users.get(id);
        if (user == null) {
            if (!data.has("username")) {
                return null;
            }
            user = new ImplUser(data, this);
        }
        return user;
    }

    /**
     * Gets the map which contains all known servers.
     *
     * @return The map which contains all known servers.
     */
    public ConcurrentHashMap<String, Server> getServerMap() {
        return servers;
    }

    /**
     * Gets the map which contains all known users.
     *
     * @return The map which contains all known users.
     */
    public ConcurrentHashMap<String, User> getUserMap() {
        return users;
    }

    /**
     * Gets the used socket adapter.
     *
     * @return The socket adapter.
     */
    public DiscordWebsocketAdapter getSocketAdapter() {
        return socketAdapter;
    }

    /**
     * Requests a new token.
     *
     * @return The requested token.
     */
    public String requestTokenBlocking() {
        try {
            logger.debug("Trying to request token (email: {}, password: {})", email, password.replaceAll(".", "*"));
            HttpResponse<JsonNode> response = Unirest.post("https://discordapp.com/api/v6/auth/login")
                    .header("User-Agent", Javacord.USER_AGENT)
                    .header("Content-Type", "application/json")
                    .body(new JSONObject().put("email", email).put("password", password).toString())
                    .asJson();
            JSONObject jsonResponse = response.getBody().getObject();
            if (response.getStatus() == 400) {
                throw new IllegalArgumentException("400 Bad request! Maybe wrong email or password? StatusText: " + response.getStatusText() + "; Body: " + response.getBody());
            }
            if (response.getStatus() < 200 || response.getStatus() > 299) {
                throw new IllegalStateException("Received http status code " + response.getStatus()
                        + " with message " + response.getStatusText() + " and body " + response.getBody());
            }
            if (jsonResponse.has("password") || jsonResponse.has("email")) {
                throw new IllegalArgumentException("Wrong email or password!");
            }
            String token = jsonResponse.getString("token");
            logger.debug("Requested token {} (email: {}, password: {})",
                    token.replaceAll(".{10}", "**********"), email, password.replaceAll(".", "*"));
            return token;
        } catch (UnirestException e) {
            logger.warn("Couldn't request token (email: {}, password: {}). Please contact the developer!",
                    email, password.replaceAll(".", "*"), e);
            return null;
        }
    }

    /**
     * Requests the gateway.
     *
     * @return The requested gateway.
     */
    public String requestGatewayBlocking() {
        try {
            logger.debug("Requesting gateway (token: {})", token.replaceAll(".{10}", "**********"));
            HttpResponse<JsonNode> response = Unirest.get("https://discordapp.com/api/v6/gateway")
                    .header("authorization", token)
                    .asJson();
            if (response.getStatus() == 401) {
                throw new IllegalStateException("Cannot request gateway! Invalid token?");
            }
            if (response.getStatus() < 200 || response.getStatus() > 299) {
                throw new IllegalStateException("Received http status code " + response.getStatus()
                        + " with message " + response.getStatusText() + " and body " + response.getBody());
            }
            String gateway = response.getBody().getObject().getString("url");
            logger.debug("Requested gateway {} (token: {})", gateway, token.replaceAll(".{10}", "**********"));
            return gateway;
        } catch (UnirestException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets a list with all registers listeners of the given class.
     *
     * @param <T> The type of the listener.
     * @param listenerClass The type of the listener.
     * @return A list with all registers listeners of the given type.
     */
    public <T extends Listener> List<T> getListeners(Class<T> listenerClass) {
        List<T> listenersList = (List<T>) listeners.get(listenerClass);
        return listenersList == null ? new ArrayList<T>() : listenersList;
    }

    /**
     * Gets a list with all registers listeners of the given generic type.
     * This method hasn't the extra listenerClass-parameter of {@link #getListeners(Class)} but is a little bot slower.
     *
     * @param <T> The type of the listener.
     * @return A list with all registers listeners of the given type.
     */
    public <T extends Listener> List<T> getListeners() {
        for (List<Listener> list : listeners.values()) {
            try {
                return (List<T>) list;
            } catch (ClassCastException ignored) {}
        }
        return new ArrayList<>();
    }

    /**
     * Adds a message to the message cache.
     *
     * @param message The message to add.
     */
    public void addMessage(Message message) {
        synchronized (messages) {
            if (messages.size() > messageCacheSize) { // only cache the last 200 messages
                messages.remove(0);
            }
            messages.add(message);
        }
    }

    /**
     * Removes a message from the cache.
     *
     * @param message The message to remove.
     */
    public void removeMessage(Message message) {
        synchronized (messages) {
            messages.remove(message);
        }
        synchronized (messageHistories) {
            for (MessageHistory history : messageHistories) {
                ((ImplMessageHistory) history).removeMessage(message.getId());
            }
        }
    }

    /**
     * Adds a history to the history list.
     *
     * @param history The history to add.
     */
    public void addHistory(MessageHistory history) {
        synchronized (messageHistories) {
            messageHistories.add(history);
        }
    }

    /**
     * Checks the response.
     *
     * @param response The response to check.
     * @throws Exception If the response has problems (status code not between 200 and 300).
     */
    public void checkResponse(HttpResponse<JsonNode> response) throws Exception {
        String message = "";
        if (response.getBody() != null && !response.getBody().isArray() &&
                response.getBody().getObject().has("message")) {
            message = " " + response.getBody().getObject().getString("message");
        }
        if (response.getStatus() == 403) {
            throw new PermissionsException("Missing permissions!" + message);
        }
        if (response.getStatus() == 429) {
            // Handled in #checkRateLimit
            return;
        }
        if (response.getStatus() < 200 || response.getStatus() > 299) {
            throw new BadResponseException("Received http status code " + response.getStatus() + " with message "
                    + response.getStatusText() + " and body " + response.getBody(), response.getStatus(),
                    response.getStatusText(), response);
        }
    }

    /**
     * Checks if there current action if rate limited. The check should be performed before AND after making a request.
     *
     * @param response The response to check. Can be <code>null</code>.
     * @param type The type of the rate limit.
     * @param server The server of the rate limit.
     * @param channel The channel of the rate limit.
     *
     * @throws RateLimitedException if there's a rate limit.
     */
    public void checkRateLimit(HttpResponse<JsonNode> response, RateLimitType type, Server server, Channel channel)
            throws RateLimitedException {
        if (rateLimitManager.isRateLimited(type, server, channel) && type != RateLimitType.UNKNOWN) {
            long retryAfter = rateLimitManager.getRateLimit(type, server, channel);
            throw new RateLimitedException(
                    "We are rate limited for " + retryAfter + " ms!", retryAfter, type, server, channel, rateLimitManager);
        }
        // {"global":false,"retry_after":104,"message":"You are being rate limited."}
        if (response != null && response.getStatus() == 429) {
            long retryAfter = response.getBody().getObject().getLong("retry_after");
            rateLimitManager.addRateLimit(type, server, channel, retryAfter);
            throw new RateLimitedException(
                    "We are rate limited for " + retryAfter + " ms (type: " + type.name() + ")!",
                    retryAfter, type, server, channel, rateLimitManager);
        }
    }

    /**
     * Gets a set with all message histories.
     *
     * @return A set with all message histories.
     */
    public Set<MessageHistory> getMessageHistories() {
        return messageHistories;
    }

    /**
     * Gets the internal used server join listener (for server creations and invite accepts).
     *
     * @return The internal used server join listener.
     */
    public ServerJoinListener getInternalServerJoinListener() {
        return listener;
    }

    /**
     * Sets the socket adapter.
     *
     * @param socketAdapter The socket adapter to set.
     */
    public void setSocketAdapter(DiscordWebsocketAdapter socketAdapter) {
        this.socketAdapter = socketAdapter;
    }

}
