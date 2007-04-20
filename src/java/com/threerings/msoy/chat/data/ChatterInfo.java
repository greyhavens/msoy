//
// $Id$

package com.threerings.msoy.chat.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains the name and avatar headshot of a chat channel participant.
 */
public class ChatterInfo extends SimpleStreamableObject
    implements DSet.Entry
{
    /** This member's name. */
    public MemberName name;

    /** This member's avatar headshot (or null for the default). */
    public MediaDesc headshot;

    // from DSet.Entry
    public Comparable getKey ()
    {
        return name;
    }

    /** Used for unserialization. */
    public ChatterInfo ()
    {
    }

    /**
     * Creates a chatter info record for the specified member.
     */
    public ChatterInfo (MemberObject memobj)
    {
        name = memobj.memberName;
        // TODO: use headshot media when we have it
        headshot = (memobj.avatar == null) ? null : memobj.avatar.getPreviewMedia();
    }
}
