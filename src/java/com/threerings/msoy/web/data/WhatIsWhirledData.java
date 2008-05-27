//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/** 
 * Contains the data for the WhatIsWhirled page.
 */
public class WhatIsWhirledData
    implements IsSerializable
{    
    /** Currently featured whirleds */
    public GroupCard[] featuredWhirleds;
    
    /** Top featured game information */
    public FeaturedGameInfo[] topGames;
    
    /** Top featured avatar information */
    public ListingCard[] topAvatars;
}
