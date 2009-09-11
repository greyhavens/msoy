//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.JavaScriptUtil;

public class StatusChangeEvent extends FlashEvent
{
    // constants for the different types of status updates we can receive. Defined in WorldClient.as
    public static final int LEVEL = 1;
    public static final int COINS = 2;
    public static final int BARS = 3;
    public static final int MAIL = 4;

    /** The name of this event type: defined in WorldClient.as. */
    public static final String NAME = "statusChange";

    public StatusChangeEvent ()
    {
    }

    /**
     * Creates a status change event for when we know the old value.
     */
    public StatusChangeEvent (int type, int value, int oldValue)
    {
        _type = type;
        _value = value;
        _oldValue = oldValue;
        _initializing = false;
    }

    /**
     * Creates a status change event to initialize a value.
     */
    public StatusChangeEvent (int type, int value)
    {
        _type = type;
        _value = value;
        _oldValue = 0;
        _initializing = true;
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
        _initializing = JavaScriptUtil.getBooleanElement(args, 3);
    }

    @Override // from FlashEvent
    public void toJSObject (JavaScriptObject args)
    {
        JavaScriptUtil.setIntElement(args, 0, _type);
        JavaScriptUtil.setIntElement(args, 1, _value);
        JavaScriptUtil.setIntElement(args, 2, _oldValue);
        JavaScriptUtil.setBooleanElement(args, 3, _initializing);
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

    /**
     * Whether this event is intializing the status value. If so, then the old value is not
     * meaningful and will be set to zero.
     */
    public boolean isInitializing ()
    {
        return _initializing;
    }

    protected int _type;
    protected int _value;
    protected int _oldValue;
    protected boolean _initializing;
}
