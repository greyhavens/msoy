//
// $Id$

package com.threerings.msoy.group.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information displayed on the main groups 'Galaxy' page.
 */
public class GalaxyData
    implements IsSerializable
{
    /** The number of featured groups to show on the Galaxy page. */
    public static final int FEATURED_GROUPS_COUNT = 16;

    /** The number of my groups to show on the Galaxy page. */
    public static final int MY_GROUPS_COUNT = 6;

    /** The new and hot groups. */
    public List<GroupCard> featuredGroups;

    /** The member's MY_GROUPS_COUNT groups. */
    public List<GroupCard> myGroups;

    /** The official whirled groups. */
    public List<GroupCard> officialGroups;
}
