//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.util.ActionScript;
import com.threerings.util.MessageBundle;

/**
 * Notifies a user that they've leveled up.
 */
public class LevelUpNotification extends Notification
{
    public LevelUpNotification ()
    {
    }

    @ActionScript(omit=true)
    public LevelUpNotification (int newLevel)
    {
        _newLevel = newLevel;
    }

    @Override // from Notification
    public boolean isPersistent ()
    {
        return false;
    }

    // from Notification
    public String getAnnouncement ()
    {
        return MessageBundle.tcompose("m.level_up", _newLevel);
    }

    /** Our new level. */
    protected int _newLevel;
}
