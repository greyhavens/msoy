//
// $Id$

package com.threerings.msoy.facebook.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.msoy.web.gwt.ServiceException;

/**
 * Provides services for munging Facebook and Whirled data for display.
 */
@RemoteServiceRelativePath(value=FacebookService.REL_PATH)
public interface FacebookService extends RemoteService
{
    public static final String ENTRY_POINT = "/fbpage";
    public static final String REL_PATH = "../../.." + FacebookService.ENTRY_POINT;

    /**
     * Retrieves the list of friends and their associated info for the currently logged in user.
     * The initial candidates for the list are those Facebook friends who also have a mapped
     * Whirled account.
     */
    List<FacebookFriendInfo> getFriends ()
        throws ServiceException;

    /**
     * Retrieves the list of facebook ids that are friends of the currently logged in user and have
     * associated whirled accounts.
     */
    List<Long> getFriendsUsingApp ()
        throws ServiceException;
}
