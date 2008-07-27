//
// $Id$

package com.threerings.msoy.group.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains a group's name, logo and home scene.
 */
public class GroupCard
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
}
