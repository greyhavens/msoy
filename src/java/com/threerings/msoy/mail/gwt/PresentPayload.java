//
// $Id$

package com.threerings.msoy.mail.gwt;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Contains information on an item gifted from one player to another.
 */
public class PresentPayload extends MailPayload
{
    /** The identifier for the item being gifted. */
    public ItemIdent ident;

    /** The name of this item. */
    public String name;

    /** This item's preview thumbnail (may be null). */
    public MediaDesc thumbMedia;

    /**
     * An empty constructor for deserialization.
     */
    public PresentPayload ()
    {
    }

    /**
     * Create a new {@link PresentPayload} with the supplied configuration.
     */
    public PresentPayload (Item item)
    {
        this.ident = item.getIdent();
        this.name = item.name;
        this.thumbMedia = item.thumbMedia;
    }

    /**
     * Returns the preview to be shown for this present.
     */
    public MediaDesc getThumbnailMedia ()
    {
        return (thumbMedia == null) ? Item.getDefaultThumbnailMediaFor(ident.type) : thumbMedia;
    }

    @Override
    public int getType ()
    {
        return MailPayload.TYPE_PRESENT;
    }
}
