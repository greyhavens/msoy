//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.item.web.ItemIdent;

/**
 * Used to coordinate the "control" of a particular scene entity. The client that is in control of
 * the entity is the only one that will be allowed to make changes to the entity's distributed
 * state.
 */
public class EntityControl extends SimpleStreamableObject
    implements DSet.Entry
{
    /** Identifies the item being controlled. */
    public ItemIdent ident;

    /** The body oid of the client currently controlling this entity. */
    public int controllerOid;

    /** Used when unserializing. */
    public EntityControl ()
    {
    }

    /** Creates a controller mapping for the specified entity. */
    public EntityControl (ItemIdent ident, int controllerOid)
    {
        this.ident = ident;
        this.controllerOid = controllerOid;
    }

    // from interface DSet.Entry
    public Comparable getKey ()
    {
        return ident;
    }
}
