//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.JavaScriptUtil;

public class ItemUsageEvent extends FlashEvent
{
    public static final String NAME = "ItemUsageChanged";

    @Override // from FlashEvent
    public String getEventName ()
    {
        return NAME;
    }

    @Override // from FlashEvent
    public void fromJSObject (JavaScriptObject args)
    {
        _type = JavaScriptUtil.getByteElement(args, 0);
        _id = JavaScriptUtil.getIntElement(args, 1);
        _usage = JavaScriptUtil.getByteElement(args, 2);
        _loc = JavaScriptUtil.getIntElement(args, 3);
    }

    @Override // from FlashEvent
    public void toJSObject (JavaScriptObject args)
    {
        JavaScriptUtil.setByteElement(args, 0, _type);
        JavaScriptUtil.setIntElement(args, 1, _id);
        JavaScriptUtil.setByteElement(args, 2, _usage);
        JavaScriptUtil.setIntElement(args, 3, _loc);
    }

    @Override // from FlashEvent
    public void notifyListener (FlashEventListener listener)
    {
        if (listener instanceof ItemUsageListener) {
            ((ItemUsageListener) listener).itemUsageChanged(this);
        }
    }

    /**
     * Get the item type of the item that was changed.
     */
    public byte getItemType ()
    {
        return _type;
    }

    /**
     * Get the item id of the item that was changed.
     */
    public int getItemId ()
    {
        return _id;
    }

    /**
     * Get the new usage constant for the changed item.
     */
    public byte getUsage ()
    {
        return _usage;
    }

    /**
     * Get the new usage location for the changed item.
     */
    public int getLocation ()
    {
        return _loc;
    }

    protected byte _type;
    protected int _id;
    protected byte _usage;
    protected int _loc;
}
