//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.FlashClients;

public class DecorChangedEvent extends FlashEvent
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
        if (listener instanceof DecorChangeListener) {
            ((DecorChangeListener) listener).decorChanged(this);
        }
    }

    public int getDecorId ()
    {
        return _newId;
    }

    public int getOldDecorId ()
    {
        return _oldId;
    }

    protected int _newId;
    protected int _oldId;
}
