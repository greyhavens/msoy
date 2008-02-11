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
    /** Info on the featured game for this genre. */
    public FeaturedGameInfo featuredGame;

    /** 
     * Information about each game in this genre.
     * 
     * @gwt.typeArgs <com.threerings.msoy.web.data.GameInfo>
     */
    public List games;
}
