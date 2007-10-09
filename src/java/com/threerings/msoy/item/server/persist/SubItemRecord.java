//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Index;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;

/**
 * Contains additional fields required by sub-items.
 */
@Entity(indices={
    @Index(name="ixSuiteId", fields={ SubItemRecord.SUITE_ID })
})
public abstract class SubItemRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #suiteId} field. */
    public static final String SUITE_ID = "suiteId";

    /** The column identifier for the {@link #ident} field. */
    public static final String IDENT = "ident";
    // AUTO-GENERATED: FIELDS END

    /** The identifier of the suite to which this subitem belongs. See {@link SubItem#suiteId}. */
    public int suiteId;

    /** An identifier for this level pack, used by the game code. */
    public String ident;

    @Override // from ItemRecord
    public void prepareForListing (ItemRecord oldListing)
    {
        super.prepareForListing(oldListing);

        if (oldListing != null) {
            // inherit our suite id from the old item
            suiteId = ((SubItemRecord)oldListing).suiteId;
        }
    }

    @Override // from ItemRecord
    public Item toItem ()
    {
        SubItem item = (SubItem)super.toItem();
        item.suiteId = suiteId;
        item.ident = ident;
        return item;
    }

    @Override // from ItemRecord
    protected void fromItem (Item item)
    {
        super.fromItem(item);

        SubItem sitem = (SubItem)item;
        suiteId = sitem.suiteId;
        ident = sitem.ident;
    }
}
