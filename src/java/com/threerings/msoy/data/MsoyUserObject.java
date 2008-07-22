//
// $Id$

package com.threerings.msoy.data;

import com.threerings.msoy.data.all.MemberName;

/**
 * Defines the interface provided by both {@link MemberObject} and {@link PlayerObject}.
 */
public interface MsoyUserObject
{
    /**
     * Returns this member's name.
     */
    MemberName getMemberName ();

    /**
     * Returns this member's unique id.
     */
    int getMemberId ();
}
