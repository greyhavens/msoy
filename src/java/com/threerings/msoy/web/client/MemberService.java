//
// $Id$

package com.threerings.msoy.web.client;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.MemberInvites;

/**
 * Defines member-specific services available to the GWT/AJAX web client.
 */
public interface MemberService extends RemoteService
{
    /**
     * Figure out whether or not a given member is your friend.
     */
    public boolean getFriendStatus (WebCreds creds, int memberId)
        throws ServiceException;

    /**
     * Accept a friend invitation.
     */
    public void addFriend (WebCreds creds, int friendId)
        throws ServiceException;

    /**
     * Remove a friend.
     */
    public void removeFriend (WebCreds creds, int friendId)
        throws ServiceException;

    /**
     * Loads all items in a player's inventory of the specified type.
     */
    public ArrayList loadInventory (WebCreds creds, byte type)
        throws ServiceException;

    /**
     * Fetch neighborhood data for a given member or group in JSON-serialized form.
     */
    public String serializeNeighborhood (WebCreds creds, int entityId, boolean forGroup)
        throws ServiceException;

    /**
     * Fetch the n most Popular Places data in JSON-serialized form.
     */
    public String serializePopularPlaces (WebCreds creds, int n)
        throws ServiceException;

    /**
     * Return the invitation details for the given creds.
     */
    public MemberInvites getInvitationsStatus (WebCreds creds) 
        throws ServiceException;
}
