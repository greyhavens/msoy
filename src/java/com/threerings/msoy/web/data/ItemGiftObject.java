//
// $Id$

package com.threerings.msoy.web.data;

import com.threerings.msoy.item.web.ItemIdent;

/**
 * This object contains a reference to an item that's included as a gift in a mail message.
 */
public class ItemGiftObject extends MailPayload
{
    /** The item reference. */
    public ItemIdent item;

    /**
     * An empty constructor for deserialization.
     */
    public ItemGiftObject ()
    {
    }

    /**
     * Create a new {@link ItemGiftObject} with the supplied configuration.
     */
    public ItemGiftObject (ItemIdent item)
    {
        this.item = item;
    }
    
    // @Override
    public int getType ()
    {
        return MailPayload.TYPE_ITEM_GIFT;
    }
}
