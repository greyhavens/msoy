//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.data.BodyObject;

import com.threerings.whirled.data.ScenePlace;

/**
 * Contains additional information for a body in Whirled.
 */
public class MsoyBodyObject extends BodyObject
{
    /** The current state of the body's actor, or null if unset/unknown/default. */
    public transient String actorState;

    /**
     * Returns the scene occupied by this body.
     *
     * @see ScenePlace#getSceneId
     */
    public int getSceneId ()
    {
        return ScenePlace.getSceneId(this);
    }
}
