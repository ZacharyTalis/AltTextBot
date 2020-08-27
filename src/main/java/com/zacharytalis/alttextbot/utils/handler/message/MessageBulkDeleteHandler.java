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
package com.zacharytalis.alttextbot.utils.handler.message;

import com.zacharytalis.alttextbot.ImplDiscordAPI;
import com.zacharytalis.alttextbot.entities.message.Message;
import com.zacharytalis.alttextbot.listener.message.MessageDeleteListener;
import com.zacharytalis.alttextbot.utils.LoggerUtil;
import com.zacharytalis.alttextbot.utils.PacketHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.List;

/**
 * Handles the message bulk delete packet.
 */
public class MessageBulkDeleteHandler extends PacketHandler {

    /**
     * The logger of this class.
     */
    private static final Logger logger = LoggerUtil.getLogger(MessageBulkDeleteHandler.class);

    /**
     * Creates a new instance of this class.
     *
     * @param api The api.
     */
    public MessageBulkDeleteHandler(ImplDiscordAPI api) {
        super(api, true, "MESSAGE_DELETE_BULK");
    }

    @Override
    public void handle(JSONObject packet) {
        JSONArray messageIds = packet.getJSONArray("ids");
        for (int i = 0; i < messageIds.length(); i++) {
            String messageId = messageIds.getString(i);
            final Message message = api.getMessageById(messageId);
            if (message == null) {
                return; // no cached version available
            }
            listenerExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    List<MessageDeleteListener> listeners = api.getListeners(MessageDeleteListener.class);
                    synchronized (listeners) {
                        for (MessageDeleteListener listener : listeners) {
                            try {
                                listener.onMessageDelete(api, message);
                            } catch (Throwable t) {
                                logger.warn("Uncaught exception in MessageDeleteListener!", t);
                            }
                        }
                    }
                }
            });
        }
    }

}
