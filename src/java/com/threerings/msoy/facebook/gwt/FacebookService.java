//
// $Id$

package com.threerings.msoy.facebook.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.gwt.ServiceException;

/**
 * Provides services for munging Facebook and Whirled data for display.
 */
public interface FacebookService extends RemoteService
{
    public static final String ENTRY_POINT = "/fbpage";

    /**
     * Retrieve the list of friends and their associated info for the currently logged in user.
     * The initial candidates for the list are those Facebook friends who also have a mapped
     * Whirled account.
     */
    List<FacebookFriendInfo> getFriends ()
        throws ServiceException;
}
