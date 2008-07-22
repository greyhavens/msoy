//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.FlashClients;

public class NameChangeEvent extends FlashEvent
{
    /** The name of this event type: defined in BaseClient.as. */
    public static final String NAME = "nameChange";

    public NameChangeEvent ()
    {
    }

    public NameChangeEvent (String newName)
    {
        _name = newName;
    }

    @Override // FlashEvent
    public String getEventName ()
    {
        return NAME;
    }

    @Override // FlashEvent
    public void readFlashArgs (JavaScriptObject args)
    {
        _name = FlashClients.getStringElement(args, 0);
    }

    @Override // FlashEvent
    public void notifyListener (FlashEventListener listener)
    {
        if (listener instanceof NameChangeListener) {
            ((NameChangeListener) listener).nameChanged(this);
        }
    }

    public String getName ()
    {
        return _name;
    }

    protected String _name;
}
