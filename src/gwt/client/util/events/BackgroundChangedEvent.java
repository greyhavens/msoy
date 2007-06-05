//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.FlashClients;

/**
 * Flash event types for all scene changes that affect background properties of which a scene
 * may have only one, such as decor and audio.
 */
public class BackgroundChangedEvent extends FlashEvent
{
    // @Override // FlashEvent
    public void readFlashArgs (JavaScriptObject args) 
    {
        if (FlashClients.getLength(args) >= 3) {
            _type = FlashClients.getIntElement(args, 0);
            _newId = FlashClients.getIntElement(args, 1);
            _oldId = FlashClients.getIntElement(args, 2);
        }
    }

    // @Override // FlashEvent
    public void notifyListener (FlashEventListener listener)
    {
        if (listener instanceof BackgroundChangeListener) {
            ((BackgroundChangeListener) listener).backgroundChanged(this);
        }
    }

    public int getType ()
    {
        return _type;
    }

    public int getBackgroundId ()
    {
        return _newId;
    }

    public int getOldBackgroundId ()
    {
        return _oldId;
    }

    protected int _type;
    protected int _newId;
    protected int _oldId;
}
