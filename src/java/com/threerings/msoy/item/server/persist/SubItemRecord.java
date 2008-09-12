//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.common.base.Function;

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

    /** A function that extracts {@link #ident}. */
    public static Function<SubItemRecord, String> GET_IDENT = new Function<SubItemRecord, String>() {
        public String apply (SubItemRecord record) {
            return record.ident;
        }
    };

    /** The identifier of the suite to which this subitem belongs. See {@link SubItem#suiteId}. */
    public int suiteId;

    /** An identifier for this level pack, used by the game code. */
    public String ident;

    /**
     * Configures this sub-item with any information that it needs from its parent.
     */
    public void initFromParent (ItemRecord parent)
    {
        // the suite id is the item id of the parent; when we get listed it will get adjusted
        suiteId = parent.itemId;
    }

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
    public void fromItem (Item item)
    {
        super.fromItem(item);

        SubItem sitem = (SubItem)item;
        ident = sitem.ident;
    }
}
