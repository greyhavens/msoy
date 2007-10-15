//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.GameDetail;
import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.PlayerRating;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.TrophyCase;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Provides game related services.
 */
public interface GameService extends RemoteService
{
    /**
     * Loads the configuration needed to play (launch) the specified game.
     */
    public LaunchConfig loadLaunchConfig (WebIdent ident, int gameId)
        throws ServiceException;

    /**
     * Loads the details for the specified game.
     */
    public GameDetail loadGameDetail (WebIdent ident, int gameId)
        throws ServiceException;

    /**
     * Loads and returns the trophies awarded by the specified game. Filling in when they were
     * earned by the caller if possible.
     *
     * @gwt.typeArgs <com.threerings.msoy.game.data.all.Trophy>
     */
    public List loadGameTrophies (WebIdent ident, int gameId)
        throws ServiceException;

    /**
     * Loads all trophies owned by the specified member.
     */
    public TrophyCase loadTrophyCase (WebIdent ident, int memberId)
        throws ServiceException;

    /**
     * Returns the top-ranked players for the specified game.
     *
     * @param onlyMyFriends if true, only the player and their friends will be included in the
     * rankings.
     *
     * @return two arrays, the first the single-player rankings for this game, the second its
     * multiplayer rankings.
     */
    public PlayerRating[][] loadTopRanked (WebIdent ident, int gameId, boolean onlyMyFriends)
        throws ServiceException;
}
