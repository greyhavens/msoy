//
// $Id$

package com.threerings.msoy.bureau.data;

import static com.threerings.msoy.Log.log;

import com.threerings.presents.data.ClientObject;
import com.whirled.bureau.data.BureauTypes;

/** Client object purely for distinguishing windows in service methods. */
public class WindowClientObject extends ClientObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>bureauId</code> field. */
    public static final String BUREAU_ID = "bureauId";
    // AUTO-GENERATED: FIELDS END

    /** The bureau id of the owner of this window. */
    public String bureauId;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>bureauId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBureauId (String value)
    {
        String ovalue = this.bureauId;
        requestAttributeChange(
            BUREAU_ID, value, ovalue);
        this.bureauId = value;
    }
    // AUTO-GENERATED: METHODS END

    /**
     * Checks if an arbitrary client is a window client and was established by the bureau for a
     * specified game id.
     */
    public static boolean isForGame (ClientObject caller, int gameId)
    {
        if (!(caller instanceof WindowClientObject)) {
            return false;
        }
        return ((WindowClientObject)caller).isForGame(gameId);
    }

    /**
     * Checks if this window client was established by the bureau for the given game id.
     */
    public boolean isForGame (int gameId)
    {
        // Fish out the game id
        if (!bureauId.startsWith(BureauTypes.GAME_BUREAU_ID_PREFIX)) {
            log.warning("Bad bureau id", "client", this);
            return false;
        }

        int thisGameId = Integer.parseInt(bureauId.substring(
            BureauTypes.GAME_BUREAU_ID_PREFIX.length()));

        return thisGameId == gameId;
    }
}
