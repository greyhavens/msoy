//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.FlashClients;

public abstract class AvatarChangedListener extends FlashEventListener
{
    // @Override FlashEventListener
    public String getEventName ()
    {
        // defined in WorldClient.as
        return "avatarChanged";
    }

    public void trigger (JavaScriptObject args) 
    {
        if (FlashClients.getLength(args) >= 2) {
            avatarChanged(FlashClients.getIntElement(args, 0), FlashClients.getIntElement(args, 1));
        }
    }

    public abstract void avatarChanged (int newAvatarId, int oldAvatarId);
}
