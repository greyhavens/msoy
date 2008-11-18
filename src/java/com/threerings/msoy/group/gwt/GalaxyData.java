//
// $Id$

package com.threerings.msoy.group.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.group.gwt.GroupService;

/**
 * Contains information displayed on the main groups 'Galaxy' page.
 */
public class GalaxyData
    implements IsSerializable
{
    /** The number of popular tags we show on the Galaxy page. */
    public static final int POPULAR_TAG_COUNT = 10;

    /** The number of my groups to show on the Galaxy page */
    public static final int MY_GROUPS_COUNT = 7;

    /** The member's MY_GROUPS_COUNT groups */
    public List<MyGroupCard> myGroups;

    /** Popular group tags. */
    public List<String> popularTags;

    /** A subset of total public groups */
    public GroupService.GroupsResult publicGroups;
}
