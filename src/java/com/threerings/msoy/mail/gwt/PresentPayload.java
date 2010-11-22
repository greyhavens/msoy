//
// $Id$

package com.threerings.msoy.mail.gwt;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.data.all.HashMediaDesc;
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
    public HashMediaDesc thumbMedia;

    /**
     * An empty constructor for deserialization.
     */
    public PresentPayload ()
    {
    }

    /**
     * Create a new {@link PresentPayload} with the supplied configuration.
     */
    public PresentPayload (ItemIdent ident, String name, MediaDesc thumb)
    {
        this.ident = ident;
        this.name = name;
        //  Because our JSON marshaller won't encode the actual type of a value, it relies on
        // data objects such as this to declare concrete types. Thus without enormous amounts
        // of trickery, our thumbMedia pretty much has to be a HashMediaType. Luckily, it will
        // always be a HashMediaDesc except in the one circumstance of an item that doesn't
        // its own thumbnail, in which case it'll be a StaticMediaDesc. We handle this case
        // by persisting an explicit null instead, and substituting in a default thumbnail in
        // getThumb() below.
        this.thumbMedia = (thumb instanceof HashMediaDesc) ? (HashMediaDesc) thumb : null;
    }

    public MediaDesc getThumb ()
    {
        return (thumbMedia != null) ? thumbMedia : Item.getDefaultThumbnailMediaFor(ident.type);
    }

    @Override
    public int getType ()
    {
        return MailPayload.TYPE_PRESENT;
    }
}
