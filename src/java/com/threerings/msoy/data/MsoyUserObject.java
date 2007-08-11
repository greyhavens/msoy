//
// $Id$

package com.threerings.msoy.data;

/**
 * Defines the interface provided by both {@link MemberObject} and {@link PlayerObject}.
 */
public interface MsoyUserObject
{
    /**
     * Returns this member's unique id.
     */
    public int getMemberId ();

    /**
     * Return our assessment of how likely this member is to be human, in [0, 1).
     */
    public float getHumanity ();
}
