//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.data.BodyObject;

/**
 * Contains additional information for a body in Whirled.
 */
@com.threerings.util.ActionScript(omit=true)
public interface MsoyBodyObject
{
    /**
     * Return the implementor itself as a BodyObject. Please use this method rather than
     * brutally casting this type to a BodyObject.
     */
    BodyObject self ();

    /**
     * Returns true if this body has an actor in the scene and as a consequence uses an
     * OccupantInfo that is instanceof ActorInfo. Currently, MOBs are not actors, and
     * MemberObjects representing anonymous viewers aren't either.
     */
    boolean isActor ();

    /** Get the current state of the body's actor, or null if unset/unknown/default. */
    String getActorState ();

    /** Set the current state of the body's actor. */
    public void setActorState (String value);
}
