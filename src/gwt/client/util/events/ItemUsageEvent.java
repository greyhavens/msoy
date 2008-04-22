//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.FlashClients;

public class ItemUsageEvent extends FlashEvent
{
    public static final String NAME = "ItemUsageChanged";

    // @Override // FlashEvent
    public String getEventName ()
    {
        return NAME;
    }

    // @Override // FlashEvent
    public void readFlashArgs (JavaScriptObject args)
    {
        _type = FlashClients.getByteElement(args, 0);
        _id = FlashClients.getIntElement(args, 1);
        _usage = FlashClients.getByteElement(args, 2);
        _loc = FlashClients.getIntElement(args, 3);
    }

    // @Override // FlashEvent
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
