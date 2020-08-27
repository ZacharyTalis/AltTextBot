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
package com.zacharytalis.alttextbot.listener.role;

import com.zacharytalis.alttextbot.DiscordAPI;
import com.zacharytalis.alttextbot.entities.permissions.Role;
import com.zacharytalis.alttextbot.listener.Listener;

/**
 * This listener listens to role Managed changes.
 */
public interface RoleChangeManagedListener extends Listener {

    /**
     * This method is called every time a role changed its Managed.
     *
     * @param api The api.
     * @param role The role with the updated managed value.
     * @param oldManaged The old managed value of the role.
     */
    public void onRoleChangeManaged(DiscordAPI api, Role role, boolean oldManaged);

}

