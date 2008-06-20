//
// $Id$

package com.threerings.msoy.web.data;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information about all games in a particular genre.
 */
public class GameGenreData
    implements IsSerializable
{
    /** Info on the featured games for this genre. */
    public FeaturedGameInfo[] featuredGames;

    /** 
     * Information about each game in this genre.
     */
    public List<GameInfo> games;
}
