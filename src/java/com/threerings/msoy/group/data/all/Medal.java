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

    public static final int MAX_DESCRIPTION_LENGTH = 200;
    public static final int MAX_NAME_LENGTH = 64;

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
}
