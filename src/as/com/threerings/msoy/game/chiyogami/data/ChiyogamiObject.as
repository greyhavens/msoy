//
// $Id$

package com.threerings.msoy.game.chiyogami.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.parlor.game.data.GameObject;

import com.threerings.msoy.item.web.MediaDesc;

/** 
 * The distributed game object for a chiyogami game.
 */
public class ChiyogamiObject extends GameObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>boss</code> field. */
    public static const BOSS :String = "boss";

    /** The field name of the <code>music</code> field. */
    public static const MUSIC :String = "music";

    /** The field name of the <code>BPM</code> field. */
    public static const BPM :String = "BPM";
    
    /** The field name of the <code>bossHealth</code> field. */
    public static const BOSS_HEALTH :String = "bossHealth";
    // AUTO-GENERATED: FIELDS END

    /** The media descriptor for the current 'boss' that everyone's
     * fighting. */
    public var host :MediaDesc;

    /** The music. */
    public var music :MediaDesc;

    /** The beats-per-minute of the music. */
    public var BPM :Number;

    /** The boss's health level. */
    public var bossHealth :Number;

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        host = (ins.readObject() as MediaDesc);
        music = (ins.readObject() as MediaDesc);
        BPM = ins.readFloat();
        bossHealth = ins.readFloat();
    }
}
}
