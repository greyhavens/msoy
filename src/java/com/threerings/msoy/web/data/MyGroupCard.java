//
// $Id: GroupCard.java 8844 2008-04-15 17:05:43Z nathan $

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.fora.data.ForumThread;
import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Detailed information on a single Group/Whirled for the "My Whirleds" page.
 */
public class MyGroupCard
    implements IsSerializable
{
    /** The group's name. */
    public GroupName name;

    /** The groups's logo (or the default). */
    public MediaDesc logo = Group.getDefaultGroupLogoMedia();

    /** This group's brief description. */
    public String blurb;

    /** The scene id of this group's hall. */
    public int homeSceneId;

    /** The number of people online in this group's scenes (as of the last snapshot). */
    public int population;
    
    /** Number of threads for this group on the My Discussions page */
    public int numUnreadThreads;
    
    /** The member's rank in the group. */
    public byte rank;
    
    /** Most recent thread for this Whirled on the My Discussions page */
    public ForumThread latestThread;
}
