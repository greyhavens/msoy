//
// $Id$

package com.threerings.msoy.game.chiyogami.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.parlor.game.data.GameObject;

import com.threerings.msoy.item.data.all.MediaDesc;

/** 
 * The distributed game object for a chiyogami game.
 */
public class ChiyogamiObject extends GameObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>bossOid</code> field. */
    public static const BOSS_OID :String = "bossOid";

    /** The field name of the <code>BPM</code> field. */
    public static const BEATS_PER_MINUTE :String = "beatsPerMinute";
    
    /** The field name of the <code>bossHealth</code> field. */
    public static const BOSS_HEALTH :String = "bossHealth";
    // AUTO-GENERATED: FIELDS END

    /** The oid of the entity in the room that is the boss. */
    public var bossOid :int;

    /** The beats-per-minute of the music. */
    public var beatsPerMinute :Number;

    /** The boss's health level. */
    public var bossHealth :Number;

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        bossOid = ins.readInt();
        beatsPerMinute = ins.readFloat();
        bossHealth = ins.readFloat();
    }
}
}
