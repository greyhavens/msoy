//
// $Id$

package com.threerings.msoy.room.data;

import javax.annotation.Generated;

import com.threerings.util.Name;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.data.MsoyBodyObject;

/**
 * Contains the distributed state associated with an AVRG MOB.
 */
@com.threerings.util.ActionScript(omit=true)
public class MobObject extends BodyObject
    implements MsoyBodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>gameId</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String GAME_ID = "gameId";

    /** The field name of the <code>ident</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String IDENT = "ident";

    /** The field name of the <code>actorState</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String ACTOR_STATE = "actorState";
    // AUTO-GENERATED: FIELDS END

    /** The gameId of the AVRG that created this MOB. */
    public int gameId;

    /** An identifier provided by the AVRG when creating this MOB. */
    public String ident;

    /** The current state of the body's actor, or null if unset/unknown/default. */
    public String actorState;

    // from MsoyBodyObject
    public BodyObject self ()
    {
        return this;
    }

    @Override // from MsoyBodyObject
    public boolean isActor ()
    {
        return false;
    }

    // from MsoyBodyObject
    public String getActorState ()
    {
        return actorState;
    }

    @Override // from BodyObject
    public OccupantInfo createOccupantInfo (PlaceObject plobj)
    {
        return new MobInfo(this, gameId, ident);
    }

    @Override // from BodyObject
    public Name getVisibleName ()
    {
        return Name.BLANK;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>gameId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setGameId (int value)
    {
        int ovalue = this.gameId;
        requestAttributeChange(
            GAME_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.gameId = value;
    }

    /**
     * Requests that the <code>ident</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setIdent (String value)
    {
        String ovalue = this.ident;
        requestAttributeChange(
            IDENT, value, ovalue);
        this.ident = value;
    }

    /**
     * Requests that the <code>actorState</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setActorState (String value)
    {
        String ovalue = this.actorState;
        requestAttributeChange(
            ACTOR_STATE, value, ovalue);
        this.actorState = value;
    }
    // AUTO-GENERATED: METHODS END
}
