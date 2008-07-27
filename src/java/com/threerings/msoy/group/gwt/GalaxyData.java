//
// $Id$

package com.threerings.msoy.group.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;


/**
 * Contains information displayed on the Galaxy page.
 */
public class GalaxyData
    implements IsSerializable
{
    /** The number of popular tags we show on the Galaxy page. */
    public static final int POPULAR_TAG_COUNT = 9;

    /** The number of featured Whirleds we show on the Galaxy page. */
    public static final int FEATURED_WHIRLED_COUNT = 5;

    /** The featured Whirleds for display at the top of the page. */
    public GroupCard[] featuredWhirleds;

    /**
     * Popular Whirled tags.
     */
    public List<String> popularTags;
}
