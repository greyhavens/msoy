//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

/**
 * Used to coordinate the "control" of some executable client content - currently either a room
 * entity (item) or an AVRG. This mechanism elevates one specific client-side instance to play the
 * role usually reserved for server-side logic.
 */
public class EntityControl extends SimpleStreamableObject
    implements DSet.Entry
{
    /** Identifies what is being controlled. */
    public Controllable controlled;

    /** The body oid of the client in control of this controllable. */
    public int controllerOid;

    /** Used when unserializing. */
    public EntityControl ()
    {
    }

    /** Creates a controller mapping for the specified entity. */
    public EntityControl (Controllable controlled, int controllerOid)
    {
        this.controllerOid = controllerOid;
        this.controlled = controlled;
    }

    // from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return controlled;
    }
}
