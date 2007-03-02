package com.threerings.msoy.game.data;

import com.threerings.ezgame.data.EZGameObject;
import com.threerings.presents.dobj.DSet;

public class MsoyGameObject extends EZGameObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>flowPerMinute</code> field. */
    public static final String FLOW_PER_MINUTE = "flowPerMinute";

    /** The field name of the <code>msoyGameService</code> field. */
    public static final String MSOY_GAME_SERVICE = "msoyGameService";
    // AUTO-GENERATED: FIELDS END

    /** The base per-minute flow rate of this game. */
    public int flowPerMinute;

    /** The whirled game services. */
    public MsoyGameMarshaller msoyGameService;

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

    /**
     * Requests that the <code>msoyGameService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setMsoyGameService (MsoyGameMarshaller value)
    {
        MsoyGameMarshaller ovalue = this.msoyGameService;
        requestAttributeChange(
            MSOY_GAME_SERVICE, value, ovalue);
        this.msoyGameService = value;
    }
    // AUTO-GENERATED: METHODS END
}
