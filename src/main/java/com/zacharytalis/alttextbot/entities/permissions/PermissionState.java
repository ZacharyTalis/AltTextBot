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
package com.zacharytalis.alttextbot.entities.permissions;

/**
 * This enum represents the state of a {@link PermissionType}.
 */
public enum PermissionState {

    /**
     * The given {@link PermissionType type} is not set.
     */
    NONE(),

    /**
     * The given {@link PermissionType type} is allowed.
     */
    ALLOWED(),

    /**
     * The given {@link PermissionType type} is denied.
     */
    DENIED(),

}
