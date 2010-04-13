//
// $Id$

package com.threerings.msoy.admin.data;

import javax.annotation.Generated;
import com.threerings.admin.data.ConfigObject;

/**
 * Contains runtime configurable subscription attributes.
 */
public class SubscriptionConfigObject extends ConfigObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>monthlyBarGrant</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String MONTHLY_BAR_GRANT = "monthlyBarGrant";

    /** The field name of the <code>specialItem</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String SPECIAL_ITEM = "specialItem";

    /** The field name of the <code>barscriptionCost</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String BARSCRIPTION_COST = "barscriptionCost";
    // AUTO-GENERATED: FIELDS END

    /** The amount we grant each month. */
    public int monthlyBarGrant = 45;

    /** An ItemType identifier for the special item we'll grant everyone.
     * It's granted to all subscribers when changed, and granted to all new subscribers
     * (unless they've already received it). Do not spuriously change!
     */
    public String specialItem = "0:0";

    /** The cost of barscribing. */
    public int barscriptionCost = 20;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>monthlyBarGrant</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setMonthlyBarGrant (int value)
    {
        int ovalue = this.monthlyBarGrant;
        requestAttributeChange(
            MONTHLY_BAR_GRANT, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.monthlyBarGrant = value;
    }

    /**
     * Requests that the <code>specialItem</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setSpecialItem (String value)
    {
        String ovalue = this.specialItem;
        requestAttributeChange(
            SPECIAL_ITEM, value, ovalue);
        this.specialItem = value;
    }

    /**
     * Requests that the <code>barscriptionCost</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setBarscriptionCost (int value)
    {
        int ovalue = this.barscriptionCost;
        requestAttributeChange(
            BARSCRIPTION_COST, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.barscriptionCost = value;
    }
    // AUTO-GENERATED: METHODS END
}
