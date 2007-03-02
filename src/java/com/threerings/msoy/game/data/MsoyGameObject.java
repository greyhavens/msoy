package com.threerings.msoy.game.data;

import com.threerings.ezgame.data.EZGameObject;
import com.threerings.presents.dobj.DSet;

public class MsoyGameObject extends EZGameObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>flowPerMinute</code> field. */
    public static final String FLOW_PER_MINUTE = "flowPerMinute";
    // AUTO-GENERATED: FIELDS END

    /** The base per-minute flow rate of this game. */
    public int flowPerMinute;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>flowPerMinute</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setFlowPerMinute (int value)
    {
        int ovalue = this.flowPerMinute;
        requestAttributeChange(
            FLOW_PER_MINUTE, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.flowPerMinute = value;
    }
    // AUTO-GENERATED: METHODS END
}
