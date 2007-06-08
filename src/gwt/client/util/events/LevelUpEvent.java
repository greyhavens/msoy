//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.FlashClients;

public class LevelUpEvent extends FlashEvent
{
    // @Override // FlashEvent
    public void readFlashArgs (JavaScriptObject args) 
    {
        _newLevel = FlashClients.getIntElement(args, 0);
        _oldLevel = FlashClients.getIntElement(args, 1);
    }

    // @Override // FlashEvent
    public void notifyListener (FlashEventListener listener)
    {
        if (listener instanceof LevelUpListener) {
            ((LevelUpListener) listener).leveledUp(this);
        }
    }

    public int getNewLevel ()
    {
        return _newLevel;
    }

    /**
     * It is not gauranteed that the user's level only changed by one, as they could have earned
     * enough flow while logged off to jump levels.
     */
    public int getOldLevel ()
    {
        return _oldLevel;
    }

    protected int _newLevel;
    protected int _oldLevel;
}
