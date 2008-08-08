//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.util.MessageBundle;

/**
 * Notifies a user that they've leveled up.
 */
public class LevelUpNotification extends Notification
{
    // from Notification
    override public function getAnnouncement () :String
    {
        return MessageBundle.tcompose("m.level_up", _newLevel);
    }

    // from Notification
    override public function getCategory () :int
    {
        return PERSONAL;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _newLevel = ins.readInt();
    }

    /** Our new level. */
    protected var _newLevel :int;
}
}
