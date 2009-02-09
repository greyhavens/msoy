//
// $Id$

package com.threerings.msoy.admin.data;

import com.threerings.admin.data.ConfigObject;

/**
 * Contains runtime configurable costs.
 *
 * Each cost is specified in coins, 0 for free, or a negative number to peg it to the value
 * of the magnitude of bars.
 */
public class CostsConfigObject extends ConfigObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>newRoom</code> field. */
    public static final String NEW_ROOM = "newRoom";

    /** The field name of the <code>newGroup</code> field. */
    public static final String NEW_GROUP = "newGroup";

    /** The field name of the <code>renameGroup</code> field. */
    public static final String RENAME_GROUP = "renameGroup";
    // AUTO-GENERATED: FIELDS END

    /** The cost of a new room. */
    public int newRoom = -1;

    /** The cost of new group. */
    public int newGroup = -3;

    /** The cost to rename a group. */
    public int renameGroup = -1;

    /**
     * Get the cost of one of the fields, above.
     */
    public static int getCost (int value, float exchangeRate)
    {
        return (value >= 0) ? value : (int)Math.ceil(-1 * value * exchangeRate);
    }

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

    /**
     * Requests that the <code>renameGroup</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setRenameGroup (int value)
    {
        int ovalue = this.renameGroup;
        requestAttributeChange(
            RENAME_GROUP, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.renameGroup = value;
    }
    // AUTO-GENERATED: METHODS END
}
