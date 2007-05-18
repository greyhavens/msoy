//
// $Id$

package com.threerings.msoy.web.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.InvitationResults;
import com.threerings.msoy.web.data.Invitation;

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
     * Fetch the n most Popular Places data in JSON-serialized form.
     */
    public String serializePopularPlaces (WebCreds creds, int n)
        throws ServiceException;

    /**
     * Return the invitation details for the given creds.
     */
    public MemberInvites getInvitationsStatus (WebCreds creds) 
        throws ServiceException;

    /** 
     * Send out some of this person's available invites.
     */
    public InvitationResults sendInvites (WebCreds creds, List addresses, String customMessage)
        throws ServiceException;

    /** 
     * Grabs the details for an Invitation for the use of the InvitationDialog.
     *
     * @param viewing If true, this will ensure that the viewed date in the database has been set.
     * If false, the viewdate will be left alone.
     */
    public Invitation getInvitation (String inviteId, boolean viewing)
        throws ServiceException;

    /**
     * Adds the email address from the given invite to the opt-out list.
     */
    public void optOut (Invitation invite)
        throws ServiceException;
}
