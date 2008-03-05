//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.io.SimpleStreamableObject;

/**
 * A reference to a client-side executable which can participate in the pseudo-server control
 * mechanism.
 * 
 * {@see EntityControl}
 */
public abstract class Controllable extends SimpleStreamableObject
    implements Comparable<Controllable>
{
    /** Determines whether or not the given body is able to control this controllable. */
    public abstract boolean isControllableBy (int bodyOid);
}
