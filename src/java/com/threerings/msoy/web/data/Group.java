//
// $Id$

package com.threerings.msoy.web.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.StaticMediaDesc;

/**
 * Contains the basic data of a group.
 */
public class Group
    implements Streamable, IsSerializable
{
    public static final byte POLICY_PUBLIC = 1;
    public static final byte POLICY_INVITE_ONLY = 2;
    public static final byte POLICY_EXCLUSIVE = 3;

    /** The unique id of this group. */
    public int groupId;

    /** The name of the group. */
    public String name;

    /** The blurb for the group. */
    public String blurb;

    /** The group's logo. */
    public MediaDesc logo;

    /** The id of the person who created the group. */
    public int creatorId;

    public Date creationDate;

    public byte policy;

    /**
     * Creates a default logo for use with groups that have no logo.
     */
    public static MediaDesc getDefaultGroupLogoMedia ()
    {
        return new StaticMediaDesc(MediaDesc.IMAGE_PNG, Item.PHOTO, "group_logo");
    }
}
