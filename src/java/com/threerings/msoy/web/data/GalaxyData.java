//
// $Id$

package com.threerings.msoy.web.data;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.group.data.Group;

/**
 * Contains information displayed on the Galaxy page.
 */
public class GalaxyData
    implements IsSerializable
{
    /** The number of popular tags we show on the galaxy page. */
    public static final int POPULAR_TAG_COUNT = 9;

    /** The featured Whirled for display at the top of the page. */
    public Group featuredWhirled;

    /**
     * Popular Whirled tags.
     * 
     * @gwt.typeArgs <java.lang.String>
     */
    public List popularTags;
}
