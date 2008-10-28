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
    /** Constant value for {@link #status}. */
    public static final byte AWAY = 3;

    /** The current state of the body's actor, or null if unset/unknown/default. */
    public String actorState;

    /**
     * Returns the scene occupied by this body.
     *
     * @see ScenePlace#getSceneId
     */
    public int getSceneId ()
    {
        return ScenePlace.getSceneId(this);
    }

    /**
     * Determines whether this body is allowed to enter the specified scene. By default all bodies
     * are allowed in all scenes. Only members are further restricted.
     */
    public boolean canEnterScene (int sceneId, int ownerId, byte ownerType, byte accessControl)
    {
        return true;
    }

    @Override // from BodyObject
    protected String getStatusTranslation ()
    {
        switch (status) {
        case AWAY:
            return "away";

        default:
            return super.getStatusTranslation();
        }
    }
}
