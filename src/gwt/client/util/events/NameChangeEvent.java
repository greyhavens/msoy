//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.JavaScriptUtil;

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

    @Override // from FlashEvent
    public String getEventName ()
    {
        return NAME;
    }

    @Override // from FlashEvent
    public void fromJSObject (JavaScriptObject args)
    {
        _name = JavaScriptUtil.getStringElement(args, 0);
    }

    @Override // from FlashEvent
    public void toJSObject (JavaScriptObject args)
    {
        JavaScriptUtil.setStringElement(args, 0, _name);
    }

    @Override // from FlashEvent
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
