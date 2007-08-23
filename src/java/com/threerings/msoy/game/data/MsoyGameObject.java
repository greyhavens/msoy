//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.ezgame.data.EZGameObject;

import com.whirled.data.WhirledGame;
import com.whirled.data.WhirledGameMarshaller;

/**
 * Maintains additional state for MSOY games.
 */
public class MsoyGameObject extends EZGameObject
    implements WhirledGame
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>whirledGameService</code> field. */
    public static final String WHIRLED_GAME_SERVICE = "whirledGameService";
    // AUTO-GENERATED: FIELDS END

    /** The whirled game services. */
    public WhirledGameMarshaller whirledGameService;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>whirledGameService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setWhirledGameService (WhirledGameMarshaller value)
    {
        WhirledGameMarshaller ovalue = this.whirledGameService;
        requestAttributeChange(
            WHIRLED_GAME_SERVICE, value, ovalue);
        this.whirledGameService = value;
    }
    // AUTO-GENERATED: METHODS END
}
