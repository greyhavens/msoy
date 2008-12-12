//
// $Id$

package com.threerings.msoy.group.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;

public class Medal
    implements IsSerializable
{
    public static final int MEDAL_WIDTH = 80;
    public static final int MEDAL_HEIGHT = 60;

    public static final int MAX_DESCRIPTION_LENGTH = 70;
    public static final int MAX_NAME_LENGTH = 25;

    /** The unique id of this medal. */
    public int medalId;

    /** The group that this medal belongs to. */
    public int groupId;

    /** The name of this medal */
    public String name;

    /** The description of this medal. */
    public String description;

    /** The media of the medal icon.  It is assumed that it conforms to the width and height
     * constants defined in this class. */
    public MediaDesc icon;

    /**
     * An empty constructor for deserialization
     */
    public Medal ()
    {
    }

    /**
     * A constructor to create an empty Medal that is attached to the given groupId.
     *
     * @param groupId The owner of this particular Medal.
     */
    public Medal (int groupId)
    {
        this.groupId = groupId;
    }
}
