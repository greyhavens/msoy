//
// $Id$

package com.threerings.msoy.group.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;

public class Medal
    implements IsSerializable, Comparable<Medal>
{
    public static final int MEDAL_WIDTH = 80;
    public static final int MEDAL_HEIGHT = 60;

    public static final int MAX_DESCRIPTION_LENGTH = 70;
    public static final int MAX_NAME_LENGTH = 25;

    /**
     * Returns a Medal that is suitable as a map key, given a medalId.
     */
    public static Medal getMapKey (int medalId)
    {
        Medal medal = new Medal();
        medal.medalId = medalId;
        return medal;
    }

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

    @Override
    public int hashCode ()
    {
        return medalId;
    }

    @Override
    public boolean equals (Object o)
    {
        if (!(o instanceof Medal)) {
            return false;
        }
        Medal other = (Medal)o;
        return other.medalId == medalId && other.name.equals(name);
    }

    /**
     * Medals are compared by String natural ordering on the name member.
     */
    public int compareTo (Medal o) {
        return name.compareTo(o.name);
    }
}
