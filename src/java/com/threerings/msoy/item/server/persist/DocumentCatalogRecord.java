//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.money.data.all.Currency;

/** Catalog Records for Documents. */
@TableGenerator(name="catalogId", pkColumnValue="DOCUMENT_CATALOG")
public class DocumentCatalogRecord extends CatalogRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<DocumentCatalogRecord> _R = DocumentCatalogRecord.class;
    public static final ColumnExp<Integer> CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp<Integer> LISTED_ITEM_ID = colexp(_R, "listedItemId");
    public static final ColumnExp<Integer> ORIGINAL_ITEM_ID = colexp(_R, "originalItemId");
    public static final ColumnExp<Timestamp> LISTED_DATE = colexp(_R, "listedDate");
    public static final ColumnExp<Currency> CURRENCY = colexp(_R, "currency");
    public static final ColumnExp<Integer> COST = colexp(_R, "cost");
    public static final ColumnExp<Integer> PRICING = colexp(_R, "pricing");
    public static final ColumnExp<Integer> SALES_TARGET = colexp(_R, "salesTarget");
    public static final ColumnExp<Integer> PURCHASES = colexp(_R, "purchases");
    public static final ColumnExp<Integer> RETURNS = colexp(_R, "returns");
    public static final ColumnExp<Integer> FAVORITE_COUNT = colexp(_R, "favoriteCount");
    public static final ColumnExp<Integer> BRAND_ID = colexp(_R, "brandId");
    public static final ColumnExp<Integer> BASIS_ID = colexp(_R, "basisId");
    public static final ColumnExp<Integer> DERIVATION_COUNT = colexp(_R, "derivationCount");
    // AUTO-GENERATED: FIELDS END

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link DocumentCatalogRecord}
     * with the supplied key values.
     */
    public static Key<DocumentCatalogRecord> getKey (int catalogId)
    {
        return newKey(_R, catalogId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(CATALOG_ID); }
    // AUTO-GENERATED: METHODS END
}
