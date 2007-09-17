//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.gwt.CatalogListing;

/** Catalog Records for Documents. */
@TableGenerator(name="catalogId", pkColumnValue="DOCUMENT_CATALOG")
public class DocumentCatalogRecord extends CatalogRecord<DocumentRecord>
{
    // AUTO-GENERATED: FIELDS START
    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(DocumentCatalogRecord.class, ITEM_ID);

    /** The qualified column identifier for the {@link #listedDate} field. */
    public static final ColumnExp LISTED_DATE_C =
        new ColumnExp(DocumentCatalogRecord.class, LISTED_DATE);

    /** The qualified column identifier for the {@link #flowCost} field. */
    public static final ColumnExp FLOW_COST_C =
        new ColumnExp(DocumentCatalogRecord.class, FLOW_COST);

    /** The qualified column identifier for the {@link #goldCost} field. */
    public static final ColumnExp GOLD_COST_C =
        new ColumnExp(DocumentCatalogRecord.class, GOLD_COST);

    /** The qualified column identifier for the {@link #rarity} field. */
    public static final ColumnExp RARITY_C =
        new ColumnExp(DocumentCatalogRecord.class, RARITY);

    /** The qualified column identifier for the {@link #purchases} field. */
    public static final ColumnExp PURCHASES_C =
        new ColumnExp(DocumentCatalogRecord.class, PURCHASES);

    /** The qualified column identifier for the {@link #returns} field. */
    public static final ColumnExp RETURNS_C =
        new ColumnExp(DocumentCatalogRecord.class, RETURNS);

    /** The qualified column identifier for the {@link #repriceCounter} field. */
    public static final ColumnExp REPRICE_COUNTER_C =
        new ColumnExp(DocumentCatalogRecord.class, REPRICE_COUNTER);
    // AUTO-GENERATED: FIELDS END

    public DocumentCatalogRecord ()
    {
        super();
    }

    protected DocumentCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #DocumentCatalogRecord}
     * with the supplied key values.
     */
    public static Key<DocumentCatalogRecord> getKey (int itemId)
    {
        return new Key<DocumentCatalogRecord>(
                DocumentCatalogRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
