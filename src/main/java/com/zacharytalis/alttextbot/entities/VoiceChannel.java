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
package com.zacharytalis.alttextbot.entities;

import com.zacharytalis.alttextbot.entities.permissions.Permissions;
import com.zacharytalis.alttextbot.entities.permissions.Role;

import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * This interface represents a voice channel.
 */
public interface VoiceChannel {

    /**
     * Gets the id of the channel.
     *
     * @return The id of the channel.
     */
    public String getId();

    /**
     * Gets the creation date of the channel.
     *
     * @return The creation date of the channel.
     */
    public Calendar getCreationDate();

    /**
     * Gets the name of the channel.
     *
     * @return The name of the channel.
     */
    public String getName();

    /**
     * Gets the position of the channel.
     *
     * @return The position of the channel.
     */
    public int getPosition();

    /**
     * Gets the server of the channel.
     *
     * @return The server of the channel.
     */
    public Server getServer();

    /**
     * Deletes the channel.
     *
     * @return A future which tells us if the deletion was successful or not.
     */
    public Future<Void> delete();

    /**
     * Gets an invite builder.
     * An invite builder is used to easily create invites.
     *
     * @return An invite builder.
     */
    public InviteBuilder getInviteBuilder();

    /**
     * Gets the overwritten permissions of an user in this channel.
     *
     * @param user The user which overwrites the permissions.
     * @return The overwritten permissions of the user.
     */
    public Permissions getOverwrittenPermissions(User user);

    /**
     * Gets the overwritten permissions of a role in this channel.
     *
     * @param role The role which overwrites the permissions.
     * @return The overwritten permissions of the role.
     */
    public Permissions getOverwrittenPermissions(Role role);

    /**
     * Updates the permissions of the given role.
     *
     * @param role The role to update.
     * @param permissions The permissions to set.
     * @return A future which tells us whether the update was successful or not.
     */
    public Future<Void> updateOverwrittenPermissions(Role role, Permissions permissions);

    /**
     * Updates the permissions of the given user.
     *
     * @param user The user to update.
     * @param permissions The permissions to set.
     * @return A future which tells us whether the update was successful or not.
     */
    public Future<Void> updateOverwrittenPermissions(User user, Permissions permissions);

    /**
     * Deletes the overwritten permissions of the given role.
     *
     * @param role The role to delete.
     * @return A future which tells us whether the deletion was successful or not.
     */
    public Future<Void> deleteOverwrittenPermissions(Role role);

    /**
     * Deletes the overwritten permissions of the given user.
     *
     * @param user The user to delete.
     * @return A future which tells us whether the deletion was successful or not.
     */
    public Future<Void> deleteOverwrittenPermissions(User user);

    /**
     * Updates the name of the channel.
     * If you want to update position too, use {@link #update(String, int)}.
     * Otherwise the first update will be overridden (except you wait for it to finish using {@link Future#get()}).
     *
     * @param newName The new name of the channel.
     * @return A future which tells us whether the update was successful or not.
     */
    public Future<Void> updateName(String newName);

    /**
     * Updates the position of the channel.
     * If you want to update the name too, use {@link #update(String, int)}.
     * Otherwise the first update will be overridden (except you wait for it to finish using {@link Future#get()}).
     *
     * @param newPosition The new position of the channel.
     * @return A future which tells us whether the update was successful or not.
     */
    public Future<Void> updatePosition(int newPosition);

    /**
     * Updates the voice channel.
     *
     * @param newName The new name of the channel.
     * @param newPosition The new topic of the channel.
     * @return A future which tells us whether the update was successful or not.
     */
    public Future<Void> update(String newName, int newPosition);

    /**
     * Returns a set of users connected to this channel.
     *
     * @return the set of users connected to this channel.
     */
    public Set<User> getConnectedUsers();

    /**
     * Gets the id of the parent category.
     *
     * @return The id of the parent category
     */
    public String getParentId();

}
