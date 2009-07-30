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
     */
    List<FacebookFriendInfo> getAppFriendsInfo ()
        throws ServiceException;

    /**
     * Retrieves the list of friends who have played the given game and their associated info for
     * the currently logged in user.
     * TODO: this is not yet being used because mochi games are totally unintegrated (no ratings or
     * trophies).
     */
    List<FacebookFriendInfo> getGameFriendsInfo (int gameId)
        throws ServiceException;

    /**
     * Retrieves the list of facebook ids that are friends of the currently logged in user and have
     * associated whirled accounts.
     */
    List<Long> getFriendsUsingApp ()
        throws ServiceException;

    /**
     * Retrieves the name of the game with the given id.
     */
    String getGameName (int gameId)
        throws ServiceException;
}
