//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.item.data.all.Document;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MsoyItemType;

/**
 * A digital item representing a simple text document.
 */
@TableGenerator(name="itemId", pkColumnValue="DOCUMENT")
public class DocumentRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<DocumentRecord> _R = DocumentRecord.class;
    public static final ColumnExp DOC_MEDIA_HASH = colexp(_R, "docMediaHash");
    public static final ColumnExp DOC_MIME_TYPE = colexp(_R, "docMimeType");
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp SOURCE_ID = colexp(_R, "sourceId");
    public static final ColumnExp CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp RATING_SUM = colexp(_R, "ratingSum");
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

    /** Update this version if you change fields specific to this derived class. */
    public static final int ITEM_VERSION = 1;

    /** This combines {@link #ITEM_VERSION} with {@link #BASE_SCHEMA_VERSION} to create a version
     * that allows us to make ItemRecord-wide changes and specific derived class changes. */
    public static final int SCHEMA_VERSION = ITEM_VERSION + BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** A hash code identifying the document media. */
    public byte[] docMediaHash;

    /** The MIME type of the {@link #docMediaHash} media. */
    public byte docMimeType;

    @Override // from ItemRecord
    public MsoyItemType getType ()
    {
        return MsoyItemType.DOCUMENT;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        Document document = (Document)item;
        if (document.docMedia != null) {
            docMediaHash = HashMediaDesc.unmakeHash(document.docMedia);
            docMimeType = document.docMedia.mimeType;
        } else {
            docMediaHash = null;
        }
    }

    @Override // from ItemRecord
    public byte[] getPrimaryMedia ()
    {
        return docMediaHash;
    }

    @Override // from ItemRecord
    protected byte getPrimaryMimeType ()
    {
        return docMimeType;
    }

    @Override // from ItemRecord
    protected void setPrimaryMedia (byte[] hash)
    {
        docMediaHash = hash;
    }

    @Override // from ItemRecord
    protected Item createItem ()
    {
        Document object = new Document();
        object.docMedia = docMediaHash == null ? null :
            new HashMediaDesc(docMediaHash, docMimeType);
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link DocumentRecord}
     * with the supplied key values.
     */
    public static Key<DocumentRecord> getKey (int itemId)
    {
        return newKey(_R, itemId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(ITEM_ID); }
    // AUTO-GENERATED: METHODS END
}
