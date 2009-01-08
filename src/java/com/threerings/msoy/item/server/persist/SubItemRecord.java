//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.common.base.Function;

import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;

/**
 * Contains additional fields required by sub-items.
 */
@Entity
public abstract class SubItemRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SubItemRecord> _R = SubItemRecord.class;
    public static final ColumnExp SUITE_ID = colexp(_R, "suiteId");
    public static final ColumnExp IDENT = colexp(_R, "ident");
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp SOURCE_ID = colexp(_R, "sourceId");
    public static final ColumnExp CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp RATING = colexp(_R, "rating");
    public static final ColumnExp RATING_COUNT = colexp(_R, "ratingCount");
    public static final ColumnExp USED = colexp(_R, "used");
    public static final ColumnExp LOCATION = colexp(_R, "location");
    public static final ColumnExp LAST_TOUCHED = colexp(_R, "lastTouched");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp MATURE = colexp(_R, "mature");
    public static final ColumnExp THUMB_MEDIA_HASH = colexp(_R, "thumbMediaHash");
    public static final ColumnExp THUMB_MIME_TYPE = colexp(_R, "thumbMimeType");
    public static final ColumnExp THUMB_CONSTRAINT = colexp(_R, "thumbConstraint");
    public static final ColumnExp FURNI_MEDIA_HASH = colexp(_R, "furniMediaHash");
    public static final ColumnExp FURNI_MIME_TYPE = colexp(_R, "furniMimeType");
    public static final ColumnExp FURNI_CONSTRAINT = colexp(_R, "furniConstraint");
    // AUTO-GENERATED: FIELDS END

    /** A function that extracts {@link #ident}. */
    public static Function<SubItemRecord, String> GET_IDENT = new Function<SubItemRecord, String>() {
        public String apply (SubItemRecord record) {
            return record.ident;
        }
    };

    /** The identifier of the suite to which this subitem belongs. See {@link SubItem#suiteId}. */
    @Index(name="ixSuiteId")
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
