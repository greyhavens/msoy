//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.data.all.MediaDesc;
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
     * Returns true if this actor has static media due to rendering limits.
     */
    public boolean isStatic ()
    {
        return (_flags & STATIC) != 0;
    }

    /**
     * Updates the media for this actor, potentially selecting static media.
     */
    public void updateMedia (MsoyBodyObject body)
    {
        RoomLocal local = body.getLocal(RoomLocal.class);
        if (local != null && local.useStaticMedia(body)) {
            useStaticMedia();
            _flags |= STATIC;
        } else {
            useDynamicMedia(body);
            _flags &= ~STATIC;
        }
    }

    protected ActorInfo (MsoyBodyObject body)
    {
        super(body);
        _state = body.actorState;
        updateMedia(body);
    }

    /** Used for unserialization. */
    protected ActorInfo ()
    {
    }

    /**
     * Configures {@link #_media} and {@link #_ident} with the appropriate static media.
     */
    protected abstract void useStaticMedia ();

    /**
     * Configures {@link #_media} and {@link #_ident} using information from the supplied body.
     */
    protected abstract void useDynamicMedia (MsoyBodyObject body);

    @Override // from SimpleStreamableObject
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", media=").append(_media).append(", ident=").append(_ident);
        buf.append(", state=").append(_state).append(", flags=").append(_flags);
    }

    protected MediaDesc _media;
    protected ItemIdent _ident;
    protected String _state;
    protected byte _flags;

    /** Bit flags used to check values in the _flags member. */
    protected static final byte STATIC = 1 << 0;
    protected static final byte MANAGER = 1 << 1; // used by MemberInfo but defined here for safety
}
