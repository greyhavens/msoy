//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.JavaScriptUtil;

public class StatusChangeEvent extends FlashEvent
{
    // constants for the different types of status updates we can receive. Defined in BaseClient.as
    public static final int LEVEL = 1;
    public static final int FLOW = 2;
    public static final int GOLD = 3;
    public static final int MAIL = 4;

    /** The name of this event type: defined in BaseClient.as. */
    public static final String NAME = "statusChange";

    public StatusChangeEvent ()
    {
    }

    public StatusChangeEvent (int type, int value, int oldValue)
    {
        _type = type;
        _value = value;
        _oldValue = oldValue;
    }

    @Override // from FlashEvent
    public String getEventName ()
    {
        return NAME;
    }

    @Override // from FlashEvent
    public void fromJSObject (JavaScriptObject args)
    {
        _type = JavaScriptUtil.getIntElement(args, 0);
        _value = JavaScriptUtil.getIntElement(args, 1);
        _oldValue = JavaScriptUtil.getIntElement(args, 2);
    }

    @Override // from FlashEvent
    public void toJSObject (JavaScriptObject args)
    {
        JavaScriptUtil.setIntElement(args, 0, _type);
        JavaScriptUtil.setIntElement(args, 1, _value);
        JavaScriptUtil.setIntElement(args, 2, _oldValue);
    }

    @Override // from FlashEvent
    public void notifyListener (FlashEventListener listener)
    {
        if (listener instanceof StatusChangeListener) {
            ((StatusChangeListener) listener).statusChanged(this);
        }
    }

    public int getType ()
    {
        return _type;
    }

    public int getValue ()
    {
        return _value;
    }

    public int getOldValue ()
    {
        return _oldValue;
    }

    protected int _type;
    protected int _value;
    protected int _oldValue;
}
