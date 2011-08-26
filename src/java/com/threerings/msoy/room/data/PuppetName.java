//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.msoy.data.all.MemberName;

/**
 * A marker class that indicates that the user is not real, it's a puppet.
 */
@com.threerings.util.ActionScript(omit=true)
public class PuppetName extends MemberName
{
    /** Deserialization. */
    public PuppetName ()
    {
    }

    /**
     * Standard constructor.
     */
    public PuppetName (String displayName, int memberId)
    {
        super(displayName, memberId);
    }
}
