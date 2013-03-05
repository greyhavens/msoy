//
// $Id$

package com.threerings.msoy.admin.data;

import javax.annotation.Generated;

import com.threerings.admin.data.ConfigObject;

/**
 * Contains runtime configurable costs.
 *
 * To interpret the value of a cost, use RuntimeConfig.getCoinCost();
 * (Each cost is specified in coins, 0 for free, or a negative number to peg it to the value
 * of the magnitude of bars.)
 */
@com.threerings.util.ActionScript(omit=true)
public class CostsConfigObject extends ConfigObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>newRoom</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String NEW_ROOM = "newRoom";

    /** The field name of the <code>newGroup</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String NEW_GROUP = "newGroup";

    /** The field name of the <code>newThemeSub</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String NEW_THEME_SUB = "newThemeSub";

    /** The field name of the <code>newThemeNonsub</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String NEW_THEME_NONSUB = "newThemeNonsub";

    /** The field name of the <code>broadcastBase</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String BROADCAST_BASE = "broadcastBase";

    /** The field name of the <code>broadcastIncrement</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String BROADCAST_INCREMENT = "broadcastIncrement";

    /** The field name of the <code>startParty</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String START_PARTY = "startParty";
    // AUTO-GENERATED: FIELDS END

    /** The cost of a new room. */
    public int newRoom = -1;

    /** The cost of new group. */
    public int newGroup = -3;

    /** The cost of new theme for a subscriber. */
    public int newThemeSub = 1000000;

    /** The cost of new theme for a non-subscriber. */
    public int newThemeNonsub = 1500000;

    /** The base cost of a paid broadcast. */
    public int broadcastBase = 1000;

    /** The increment for each recent broadcast. */
    public int broadcastIncrement = 4000;

    /** The cost to start a party. */
    public int startParty = 2000;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>newRoom</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setNewGroup (int value)
    {
        int ovalue = this.newGroup;
        requestAttributeChange(
            NEW_GROUP, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.newGroup = value;
    }

    /**
     * Requests that the <code>newThemeSub</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setNewThemeSub (int value)
    {
        int ovalue = this.newThemeSub;
        requestAttributeChange(
            NEW_THEME_SUB, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.newThemeSub = value;
    }

    /**
     * Requests that the <code>newThemeNonsub</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setNewThemeNonsub (int value)
    {
        int ovalue = this.newThemeNonsub;
        requestAttributeChange(
            NEW_THEME_NONSUB, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.newThemeNonsub = value;
    }

    /**
     * Requests that the <code>broadcastBase</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setBroadcastBase (int value)
    {
        int ovalue = this.broadcastBase;
        requestAttributeChange(
            BROADCAST_BASE, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.broadcastBase = value;
    }

    /**
     * Requests that the <code>broadcastIncrement</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setBroadcastIncrement (int value)
    {
        int ovalue = this.broadcastIncrement;
        requestAttributeChange(
            BROADCAST_INCREMENT, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.broadcastIncrement = value;
    }

    /**
     * Requests that the <code>startParty</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setStartParty (int value)
    {
        int ovalue = this.startParty;
        requestAttributeChange(
            START_PARTY, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.startParty = value;
    }
    // AUTO-GENERATED: METHODS END
}
