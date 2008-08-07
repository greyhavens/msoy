//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.msoy.data.MemberObject;

/**
 * A reference to a client-side executable which can participate in the pseudo-server control
 * mechanism.
 *
 * {@see EntityControl}
 */
public abstract class Controllable extends SimpleStreamableObject
    implements Comparable<Controllable>
{
    /** Determines whether or not the given member is able to control this controllable. */
    public abstract boolean isControllableBy (MemberObject member);
}
