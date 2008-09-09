//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/** Catalog Records for Avatars. */
@TableGenerator(name="catalogId", pkColumnValue="AVATAR_CATALOG")
public class AvatarCatalogRecord extends CatalogRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The qualified column identifier for the {@link #catalogId} field. */
    public static final ColumnExp CATALOG_ID_C =
        new ColumnExp(AvatarCatalogRecord.class, CATALOG_ID);

    /** The qualified column identifier for the {@link #listedItemId} field. */
    public static final ColumnExp LISTED_ITEM_ID_C =
        new ColumnExp(AvatarCatalogRecord.class, LISTED_ITEM_ID);

    /** The qualified column identifier for the {@link #originalItemId} field. */
    public static final ColumnExp ORIGINAL_ITEM_ID_C =
        new ColumnExp(AvatarCatalogRecord.class, ORIGINAL_ITEM_ID);

    /** The qualified column identifier for the {@link #listedDate} field. */
    public static final ColumnExp LISTED_DATE_C =
        new ColumnExp(AvatarCatalogRecord.class, LISTED_DATE);

    /** The qualified column identifier for the {@link #currency} field. */
    public static final ColumnExp CURRENCY_C =
        new ColumnExp(AvatarCatalogRecord.class, CURRENCY);

    /** The qualified column identifier for the {@link #cost} field. */
    public static final ColumnExp COST_C =
        new ColumnExp(AvatarCatalogRecord.class, COST);

    /** The qualified column identifier for the {@link #pricing} field. */
    public static final ColumnExp PRICING_C =
        new ColumnExp(AvatarCatalogRecord.class, PRICING);

    /** The qualified column identifier for the {@link #salesTarget} field. */
    public static final ColumnExp SALES_TARGET_C =
        new ColumnExp(AvatarCatalogRecord.class, SALES_TARGET);

    /** The qualified column identifier for the {@link #purchases} field. */
    public static final ColumnExp PURCHASES_C =
        new ColumnExp(AvatarCatalogRecord.class, PURCHASES);

    /** The qualified column identifier for the {@link #returns} field. */
    public static final ColumnExp RETURNS_C =
        new ColumnExp(AvatarCatalogRecord.class, RETURNS);

    /** The qualified column identifier for the {@link #favoriteCount} field. */
    public static final ColumnExp FAVORITE_COUNT_C =
        new ColumnExp(AvatarCatalogRecord.class, FAVORITE_COUNT);
    // AUTO-GENERATED: FIELDS END

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #AvatarCatalogRecord}
     * with the supplied key values.
     */
    public static Key<AvatarCatalogRecord> getKey (int catalogId)
    {
        return new Key<AvatarCatalogRecord>(
                AvatarCatalogRecord.class,
                new String[] { CATALOG_ID },
                new Comparable[] { catalogId });
    }
    // AUTO-GENERATED: METHODS END
}
