//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.ezgame.data.EZGameObject;

/**
 * A game config for an in-world game.
 */
public class WorldGameObject extends EZGameObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>config</code> field. */
    public static final String CONFIG = "config";
    // AUTO-GENERATED: FIELDS END

    /** The game configuration. */
    public WorldGameConfig config;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>config</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setConfig (WorldGameConfig value)
    {
        WorldGameConfig ovalue = this.config;
        requestAttributeChange(
            CONFIG, value, ovalue);
        this.config = value;
    }
    // AUTO-GENERATED: METHODS END
}
