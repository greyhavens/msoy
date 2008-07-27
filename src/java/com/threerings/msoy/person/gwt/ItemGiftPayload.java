//
// $Id$

package com.threerings.msoy.person.gwt;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * This object contains a reference to an item that's included as a gift in a mail message.
 *
 * <em>Note:</em> Don't use this, it has been replaced by {@link PresentPayload}.
 */
public class ItemGiftPayload extends MailPayload
{
    /** The item reference. */
    public ItemIdent item;

    /**
     * An empty constructor for deserialization.
     */
    public ItemGiftPayload ()
    {
    }

    /**
     * Create a new {@link ItemGiftPayload} with the supplied configuration.
     */
    public ItemGiftPayload (ItemIdent item)
    {
        this.item = item;
    }

    @Override
    public int getType ()
    {
        return MailPayload.TYPE_ITEM_GIFT;
    }
}
