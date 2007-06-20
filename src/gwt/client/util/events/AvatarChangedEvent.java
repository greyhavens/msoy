//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.FlashClients;

public class AvatarChangedEvent extends FlashEvent
{
    /** The name of this event type: defined in WorldClient.as. */
    public static final String NAME = "avatarChanged";

    // @Override // FlashEvent
    public String getEventName ()
    {
        return NAME;
    }

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
        if (listener instanceof AvatarChangeListener) {
            ((AvatarChangeListener) listener).avatarChanged(this);
        }
    }

    public int getAvatarId ()
    {
        return _newId;
    }

    public int getOldAvatarId ()
    {
        return _oldId;
    }

    protected int _newId;
    protected int _oldId;
}
