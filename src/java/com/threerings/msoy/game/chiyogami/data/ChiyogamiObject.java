//
// $Id$

package com.threerings.msoy.game.chiyogami.data;

import com.threerings.parlor.game.data.GameObject;

import com.threerings.msoy.item.web.MediaDesc;

/**
 * The distributed game object for a chiyogami game.
 */
public class ChiyogamiObject extends GameObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>boss</code> field. */
    public static final String BOSS = "boss";

    /** The field name of the <code>music</code> field. */
    public static final String MUSIC = "music";

    /** The field name of the <code>beatsPerMinute</code> field. */
    public static final String BEATS_PER_MINUTE = "beatsPerMinute";

    /** The field name of the <code>bossHealth</code> field. */
    public static final String BOSS_HEALTH = "bossHealth";
    // AUTO-GENERATED: FIELDS END

    /** The media descriptor for the current 'boss' that everyone's
     * fighting. */
    public MediaDesc boss;

    /** The music. */
    public MediaDesc music;

    /** The beats-per-minute of the music. */
    public float beatsPerMinute;

    /** The boss's health level. */
    public float bossHealth;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>boss</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBoss (MediaDesc value)
    {
        MediaDesc ovalue = this.boss;
        requestAttributeChange(
            BOSS, value, ovalue);
        this.boss = value;
    }

    /**
     * Requests that the <code>music</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setMusic (MediaDesc value)
    {
        MediaDesc ovalue = this.music;
        requestAttributeChange(
            MUSIC, value, ovalue);
        this.music = value;
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
