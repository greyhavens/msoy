//
// $Id$

package com.threerings.msoy.admin.data;

import com.threerings.admin.data.ConfigObject;

/**
 * Contains runtime configurable costs.
 *
 * To interpret the value of a cost, use RuntimeConfig.getCoinCost();
 * (Each cost is specified in coins, 0 for free, or a negative number to peg it to the value
 * of the magnitude of bars.)
 */
public class CostsConfigObject extends ConfigObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>newRoom</code> field. */
    public static final String NEW_ROOM = "newRoom";

    /** The field name of the <code>newGroup</code> field. */
    public static final String NEW_GROUP = "newGroup";
    // AUTO-GENERATED: FIELDS END

    /** The cost of a new room. */
    public int newRoom = -1;

    /** The cost of new group. */
    public int newGroup = -3;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>newRoom</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setNewRoom (int value)
    {
        int ovalue = this.newRoom;
        requestAttributeChange(
            NEW_ROOM, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.newRoom = value;
    }

    /**
     * Requests that the <code>newGroup</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setNewGroup (int value)
    {
        int ovalue = this.newGroup;
        requestAttributeChange(
            NEW_GROUP, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.newGroup = value;
    }
    // AUTO-GENERATED: METHODS END
}
