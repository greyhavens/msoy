//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.util.Name;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.data.MsoyBodyObject;

/**
 * Contains the distributed state associated with an AVRG MOB.
 */
public class MobObject extends MsoyBodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>gameId</code> field. */
    public static final String GAME_ID = "gameId";

    /** The field name of the <code>ident</code> field. */
    public static final String IDENT = "ident";
    // AUTO-GENERATED: FIELDS END

    /** The gameId of the AVRG that created this MOB. */
    public int gameId;

    /** An identifier provided by the AVRG when creating this MOB. */
    public String ident;

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
    public void setIdent (String value)
    {
        String ovalue = this.ident;
        requestAttributeChange(
            IDENT, value, ovalue);
        this.ident = value;
    }
    // AUTO-GENERATED: METHODS END
}
