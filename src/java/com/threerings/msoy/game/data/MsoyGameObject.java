package com.threerings.msoy.game.data;

import com.threerings.ezgame.data.EZGameObject;
import com.threerings.msoy.game.server.FlowRate;
import com.threerings.presents.dobj.DSet;

public class MsoyGameObject extends EZGameObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>flowRates</code> field. */
    public static final String FLOW_RATES = "flowRates";
    // AUTO-GENERATED: FIELDS END

    /** A set of flow rates per player. */
    public DSet<FlowRate> flowRates;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the specified entry be added to the
     * <code>flowRates</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToFlowRates (FlowRate elem)
    {
        requestEntryAdd(FLOW_RATES, flowRates, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>flowRates</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromFlowRates (Comparable key)
    {
        requestEntryRemove(FLOW_RATES, flowRates, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>flowRates</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateFlowRates (FlowRate elem)
    {
        requestEntryUpdate(FLOW_RATES, flowRates, elem);
    }

    /**
     * Requests that the <code>flowRates</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setFlowRates (DSet<com.threerings.msoy.game.server.FlowRate> value)
    {
        requestAttributeChange(FLOW_RATES, value, this.flowRates);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.game.server.FlowRate> clone =
            (value == null) ? null : value.typedClone();
        this.flowRates = clone;
    }
    // AUTO-GENERATED: METHODS END
}
