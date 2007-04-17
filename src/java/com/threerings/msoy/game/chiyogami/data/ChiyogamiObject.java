//
// $Id$

package com.threerings.msoy.game.chiyogami.data;

import com.threerings.parlor.game.data.GameObject;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * The distributed game object for a chiyogami game.
 */
public class ChiyogamiObject extends GameObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>bossOid</code> field. */
    public static final String BOSS_OID = "bossOid";

    /** The field name of the <code>beatsPerMinute</code> field. */
    public static final String BEATS_PER_MINUTE = "beatsPerMinute";

    /** The field name of the <code>bossHealth</code> field. */
    public static final String BOSS_HEALTH = "bossHealth";
    // AUTO-GENERATED: FIELDS END

    /** The oid of the entity in the room that is the boss. */
    public int bossOid;

    /** The beats-per-minute of the music. */
    public float beatsPerMinute;

    /** The boss's health level. */
    public float bossHealth;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>bossOid</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBossOid (int value)
    {
        int ovalue = this.bossOid;
        requestAttributeChange(
            BOSS_OID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.bossOid = value;
    }

    /**
     * Requests that the <code>beatsPerMinute</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBeatsPerMinute (float value)
    {
        float ovalue = this.beatsPerMinute;
        requestAttributeChange(
            BEATS_PER_MINUTE, Float.valueOf(value), Float.valueOf(ovalue));
        this.beatsPerMinute = value;
    }

    /**
     * Requests that the <code>bossHealth</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBossHealth (float value)
    {
        float ovalue = this.bossHealth;
        requestAttributeChange(
            BOSS_HEALTH, Float.valueOf(value), Float.valueOf(ovalue));
        this.bossHealth = value;
    }
    // AUTO-GENERATED: METHODS END
}
