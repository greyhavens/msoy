//
// $Id$

package com.threerings.msoy.web.data;

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

    /** The scene id of this group's hall. */
    public int homeSceneId;
}
