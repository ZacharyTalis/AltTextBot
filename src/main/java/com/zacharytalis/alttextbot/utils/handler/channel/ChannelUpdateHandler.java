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
import com.zacharytalis.alttextbot.entities.impl.ImplVoiceChannel;
import com.zacharytalis.alttextbot.entities.permissions.Permissions;
import com.zacharytalis.alttextbot.entities.permissions.Role;
import com.zacharytalis.alttextbot.entities.permissions.impl.ImplPermissions;
import com.zacharytalis.alttextbot.entities.permissions.impl.ImplRole;
import com.zacharytalis.alttextbot.listener.channel.ChannelChangeNameListener;
import com.zacharytalis.alttextbot.listener.channel.ChannelChangePositionListener;
import com.zacharytalis.alttextbot.listener.channel.ChannelChangeTopicListener;
import com.zacharytalis.alttextbot.listener.role.RoleChangeOverwrittenPermissionsListener;
import com.zacharytalis.alttextbot.listener.user.UserChangeOverwrittenPermissionsListener;
import com.zacharytalis.alttextbot.listener.voicechannel.VoiceChannelChangeNameListener;
import com.zacharytalis.alttextbot.listener.voicechannel.VoiceChannelChangePositionListener;
import com.zacharytalis.alttextbot.utils.LoggerUtil;
import com.zacharytalis.alttextbot.utils.PacketHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Handles the channel update packet.
 */
public class ChannelUpdateHandler extends PacketHandler {

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerUtil.getLogger(ChannelUpdateHandler.class);

    /**
     * Creates a new instance of this class.
     *
     * @param api The api.
     */
    public ChannelUpdateHandler(ImplDiscordAPI api) {
        super(api, true, "CHANNEL_UPDATE");
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
            return; // no channel with the given id was found
        }

        String name = packet.getString("name");
        if (!channel.getName().equals(name)) {
            final String oldName = channel.getName();
            ((ImplChannel) channel).setName(name);
            listenerExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    List<ChannelChangeNameListener> listeners = api.getListeners(ChannelChangeNameListener.class);
                    synchronized (listeners) {
                        for (ChannelChangeNameListener listener : listeners) {
                            try {
                                listener.onChannelChangeName(api, channel, oldName);
                            } catch (Throwable t) {
                                logger.warn("Uncaught exception in ChannelChangeNameListener!", t);
                            }
                        }
                    }
                }
            });
        }


        String topic = packet.get("topic") == null ? null : packet.get("topic").toString();
        if ((channel.getTopic() != null && topic == null)
                || (channel.getTopic() == null && topic != null)
                || (channel.getTopic() != null && !channel.getTopic().equals(topic))) {
            final String oldTopic = channel.getTopic();
            ((ImplChannel) channel).setTopic(topic);
            listenerExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    List<ChannelChangeTopicListener> listeners = api.getListeners(ChannelChangeTopicListener.class);
                    synchronized (listeners) {
                        for (ChannelChangeTopicListener listener : listeners) {
                            try {
                                listener.onChannelChangeTopic(api, channel, oldTopic);
                            } catch (Throwable t) {
                                logger.warn("Uncaught exception in ChannelChangeTopicListener!", t);
                            }
                        }
                    }
                }
            });
        }

        int position = packet.getInt("position");
        if (channel.getPosition() != position) {
            final int oldPosition = channel.getPosition();
            ((ImplChannel) channel).setPosition(position);
            listenerExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    List<ChannelChangePositionListener> listeners =
                            api.getListeners(ChannelChangePositionListener.class);
                    synchronized (listeners) {
                        for (ChannelChangePositionListener listener : listeners) {
                            try {
                                listener.onChannelChangePosition(api, channel, oldPosition);
                            } catch (Throwable t) {
                                logger.warn("Uncaught exception in ChannelChangePositionListener!", t);
                            }
                        }
                    }
                }
            });
        }

        JSONArray permissionOverwrites = packet.getJSONArray("permission_overwrites");
        for (int i = 0; i < permissionOverwrites.length(); i++) {
            JSONObject permissionOverwrite = permissionOverwrites.getJSONObject(i);
            int allow = permissionOverwrite.getInt("allow");
            int deny = permissionOverwrite.getInt("deny");
            String id = permissionOverwrite.getString("id");
            String type = permissionOverwrite.getString("type");

            // permissions overwritten by users
            if (type.equals("member")) {
                final User user;
                try {
                    user = api.getUserById(id).get();
                } catch (InterruptedException | ExecutionException e) {
                    continue;
                }
                ImplPermissions permissions = new ImplPermissions(allow, deny);
                final Permissions oldPermissions = channel.getOverwrittenPermissions(user);
                if (!oldPermissions.equals(permissions)) {
                    ((ImplChannel) channel).setOverwrittenPermissions(user, permissions);
                    listenerExecutorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            List<UserChangeOverwrittenPermissionsListener> listeners =
                                    api.getListeners(UserChangeOverwrittenPermissionsListener.class);
                            synchronized (listeners) {
                                for (UserChangeOverwrittenPermissionsListener listener : listeners) {
                                    try {
                                        listener.onUserChangeOverwrittenPermissions(api, user, channel, oldPermissions);
                                    } catch (Throwable t) {
                                        logger.warn(
                                                "Uncaught exception in UserChangeOverwrittenPermissionsListener!", t);
                                    }
                                }
                            }
                        }
                    });
                }

            }

            // permissions overwritten by roles
            if (type.equals("role")) {
                final Role role = channel.getServer().getRoleById(id);
                ImplPermissions permissions = new ImplPermissions(allow, deny);
                final Permissions oldPermissions = role.getOverwrittenPermissions(channel);
                if (!permissions.equals(oldPermissions)) {
                    ((ImplRole) role).setOverwrittenPermissions(channel, permissions);
                    listenerExecutorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            List<RoleChangeOverwrittenPermissionsListener> listeners =
                                    api.getListeners(RoleChangeOverwrittenPermissionsListener.class);
                            synchronized (listeners) {
                                for (RoleChangeOverwrittenPermissionsListener listener : listeners) {
                                    try {
                                        listener.onRoleChangeOverwrittenPermissions(api, role, channel, oldPermissions);
                                    } catch (Throwable t) {
                                        logger.warn(
                                                "Uncaught exception in RoleChangeOverwrittenPermissionsListener!", t);
                                    }
                                }
                            }
                        }
                    });
                }
            }
        }

        String parentId = packet.isNull("parent_id") ? null : packet.getString("parent_id");

        if (!Objects.deepEquals(channel.getParentId(), parentId)) {
            ((ImplChannel) channel).setParentId(parentId);
        }
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
            return; // no channel with the given id was found
        }

        String name = packet.getString("name");
        if (!channel.getName().equals(name)) {
            final String oldName = channel.getName();
            ((ImplVoiceChannel) channel).setName(name);
            listenerExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    List<VoiceChannelChangeNameListener> listeners =
                            api.getListeners(VoiceChannelChangeNameListener.class);
                    synchronized (listeners) {
                        for (VoiceChannelChangeNameListener listener : listeners) {
                            try {
                                listener.onVoiceChannelChangeName(api, channel, oldName);
                            } catch (Throwable t) {
                                logger.warn("Uncaught exception in VoiceChannelChangeNameListener!", t);
                            }
                        }
                    }
                }
            });
        }

        int position = packet.getInt("position");
        if (channel.getPosition() != position) {
            final int oldPosition = channel.getPosition();
            ((ImplVoiceChannel) channel).setPosition(position);
            listenerExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    List<VoiceChannelChangePositionListener> listeners =
                            api.getListeners(VoiceChannelChangePositionListener.class);
                    synchronized (listeners) {
                        for (VoiceChannelChangePositionListener listener : listeners) {
                            try {
                                listener.onVoiceChannelChangePosition(api, channel, oldPosition);
                            } catch (Throwable t) {
                                logger.warn("Uncaught exception in VoiceChannelChangePositionListener!", t);
                            }
                        }
                    }
                }
            });
        }

        JSONArray permissionOverwrites = packet.getJSONArray("permission_overwrites");
        for (int i = 0; i < permissionOverwrites.length(); i++) {
            JSONObject permissionOverwrite = permissionOverwrites.getJSONObject(i);
            int allow = permissionOverwrite.getInt("allow");
            int deny = permissionOverwrite.getInt("deny");
            String id = permissionOverwrite.getString("id");
            String type = permissionOverwrite.getString("type");

            // permissions overwritten by users
            if (type.equals("member")) {
                final User user;
                try {
                    user = api.getUserById(id).get();
                } catch (InterruptedException | ExecutionException e) {
                    continue;
                }
                ImplPermissions permissions = new ImplPermissions(allow, deny);
                final Permissions oldPermissions = channel.getOverwrittenPermissions(user);
                if (!oldPermissions.equals(permissions)) {
                    ((ImplVoiceChannel) channel).setOverwrittenPermissions(user, permissions);
                    listenerExecutorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            List<UserChangeOverwrittenPermissionsListener> listeners =
                                    api.getListeners(UserChangeOverwrittenPermissionsListener.class);
                            synchronized (listeners) {
                                for (UserChangeOverwrittenPermissionsListener listener : listeners) {
                                    try {
                                        listener.onUserChangeOverwrittenPermissions(api, user, channel, oldPermissions);
                                    } catch (Throwable t) {
                                        logger.warn(
                                                "Uncaught exception in UserChangeOverwrittenPermissionsListener!", t);
                                    }
                                }
                            }
                        }
                    });
                }

            }

            // permissions overwritten by roles
            if (type.equals("role")) {
                final Role role = channel.getServer().getRoleById(id);
                ImplPermissions permissions = new ImplPermissions(allow, deny);
                final Permissions oldPermissions = role.getOverwrittenPermissions(channel);
                if (!permissions.equals(oldPermissions)) {
                    ((ImplRole) role).setOverwrittenPermissions(channel, permissions);
                    listenerExecutorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            List<RoleChangeOverwrittenPermissionsListener> listeners =
                                    api.getListeners(RoleChangeOverwrittenPermissionsListener.class);
                            synchronized (listeners) {
                                for (RoleChangeOverwrittenPermissionsListener listener : listeners) {
                                    try {
                                     listener.onRoleChangeOverwrittenPermissions(api, role, channel, oldPermissions);
                                    } catch (Throwable t) {
                                        logger.warn(
                                                "Uncaught exception in RoleChangeOverwrittenPermissionsListener!", t);
                                    }
                                }
                            }
                        }
                    });
                }
            }
        }

        String parentId = packet.isNull("parent_id") ? null : packet.getString("parent_id");

        if (!Objects.deepEquals(channel.getParentId(), parentId)) {
            ((ImplVoiceChannel) channel).setParentId(parentId);
        }
    }

}
