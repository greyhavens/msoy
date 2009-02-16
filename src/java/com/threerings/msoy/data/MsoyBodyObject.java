//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.data.ScenePlace;

import com.threerings.msoy.room.data.RoomLocal;

/**
 * Contains additional information for a body in Whirled.
 */
public class MsoyBodyObject extends BodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>actorState</code> field. */
    public static final String ACTOR_STATE = "actorState";
    // AUTO-GENERATED: FIELDS END

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

    /**
     * Returns true if this body has an actor in the scene and as a consequence uses an
     * OccupantInfo that is instanceof ActorInfo.
     */
    public boolean isActor ()
    {
        return true;
    }

    @Override // from MsoyBodyObject
    public void didLeavePlace (PlaceObject plobj)
    {
        super.didLeavePlace(plobj);

        // clear out our RoomLocal when we leave our room
        setLocal(RoomLocal.class, null);
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

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>actorState</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setActorState (String value)
    {
        String ovalue = this.actorState;
        requestAttributeChange(
            ACTOR_STATE, value, ovalue);
        this.actorState = value;
    }
    // AUTO-GENERATED: METHODS END
}
