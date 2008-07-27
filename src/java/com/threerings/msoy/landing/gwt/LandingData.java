//
// $Id$

package com.threerings.msoy.landing.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.msoy.game.gwt.FeaturedGameInfo;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.item.gwt.ListingCard;

/**
 * Contains the data for the new visitor landing page.
 */
public class LandingData
    implements IsSerializable
{
    /** Currently featured whirleds */
    public GroupCard[] featuredWhirleds;

    /** Top featured game information */
    public FeaturedGameInfo[] topGames;

    /** Top featured avatar information */
    public ListingCard[] topAvatars;
}
