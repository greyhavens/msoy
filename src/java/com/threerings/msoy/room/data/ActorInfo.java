//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Contains published information about an actor in a scene (members and pets).
 */
public abstract class ActorInfo extends OccupantInfo
{
    /**
     * Returns the media that is used to display this actor.
     */
    public MediaDesc getMedia ()
    {
        return _media;
    }

    /**
     * Returns the item identifier that is used to identify this actor.
     */
    public ItemIdent getItemIdent ()
    {
        return _ident;
    }

    /**
     * Return the current state of the actor, which may be null.
     */
    public String getState ()
    {
        return _state;
    }

    /**
     * Updates the state of this actor. The actor must be republished to the room for the state
     * change to take effect.
     */
    public void setState (String state)
    {
        _state = state;
    }
    
    /**
     * Sets this actor to use simpler media for when rendering limits are in place.
     */
    public void useStaticMedia ()
    {
        _media = getStaticMedia();
        _ident = new ItemIdent(Item.OCCUPANT, getBodyOid());
        _flags |= STATIC;
    }

    /**
     * Returns true if this actor has static media due to rendering limits.
     */
    public boolean isStatic ()
    {
        return (_flags & STATIC) != 0;
    }

    protected ActorInfo (MsoyBodyObject body, MediaDesc media, ItemIdent ident)
    {
        super(body);
        _media = media;
        _ident = ident;
        _state = body.actorState;
    }

    /** Used for unserialization. */
    protected ActorInfo ()
    {
    }

    @Override // from SimpleStreamableObject
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", media=").append(_media).append(", ident=").append(_ident);
        buf.append(", state=").append(_state).append(", flags=").append(_flags);
    }

    /**
     * Gets the media to use when a rendering limit is in effect for this actor.
     */
    abstract protected MediaDesc getStaticMedia ();

    protected MediaDesc _media;
    protected ItemIdent _ident;
    protected String _state;
    protected byte _flags;

    protected static final byte STATIC = 1;
    protected static final byte MANAGER = 2; // used by MemberInfo but defined here for safety
}
