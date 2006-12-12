//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.MemberName;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Defines member-specific services available to the GWT/AJAX web client.
 */
public interface MemberService extends RemoteService
{
    /**
     * Look up a member by id and return their current name.
     */
    public MemberName getName (int memberId)
        throws ServiceException;
    
    /**
     * Figure out whether or not a given member is online.
     */
    public boolean isOnline (int memberId)
        throws ServiceException;
    
    /**
     * Look up a member by id and return the id of their home scene.
     */
    public Integer getMemberHomeId (WebCreds creds, int memberId)
        throws ServiceException;

    /**
     * Invite somebody to be your friend.
     */
    public void inviteFriend (WebCreds creds, int friendId)
        throws ServiceException;

    /**
     * Accept a friend invitation.
     */
    public void acceptFriend (WebCreds creds, int friendId)
        throws ServiceException;

    /**
     * Decline a friend invitation.
     */
    public void declineFriend (WebCreds creds, int friendId)
        throws ServiceException;
    
    /**
     * Fetch neighborhood data for a given member in JSON-serialized form.
     */
    public String serializeNeighborhood (WebCreds creds, int memberId)
        throws ServiceException;

    /**
     * Fetch the n most Popular Places data in JSON-serialized form.
     */
    public String serializePopularPlaces (WebCreds creds, int n)
        throws ServiceException;
}
