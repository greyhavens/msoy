//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.FlashClients;

public class RoomAudioChangedEvent extends FlashEvent
{
    // @Override // FlashEvent
    public void readFlashArgs (JavaScriptObject args) 
    {
        if (FlashClients.getLength(args) >= 2) {
            _newId = FlashClients.getIntElement(args, 0);
            _oldId = FlashClients.getIntElement(args, 1);
        }
    }

    // @Override // FlashEvent
    public void notifyListener (FlashEventListener listener)
    {
        if (listener instanceof RoomAudioChangeListener) {
            ((RoomAudioChangeListener) listener).audioChanged(this);
        }
    }

    public int getAudioId ()
    {
        return _newId;
    }

    public int getOldAudioId ()
    {
        return _oldId;
    }

    protected int _newId;
    protected int _oldId;
}
