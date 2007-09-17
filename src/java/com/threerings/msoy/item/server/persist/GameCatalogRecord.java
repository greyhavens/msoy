//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.gwt.CatalogListing;

/** Catalog Records for Games. */
@TableGenerator(name="catalogId", pkColumnValue="GAME_CATALOG")
public class GameCatalogRecord extends CatalogRecord<GameRecord>
{
    // AUTO-GENERATED: FIELDS START
    /** The qualified column identifier for the {@link #listedDate} field. */
    public static final ColumnExp LISTED_DATE_C =
        new ColumnExp(GameCatalogRecord.class, LISTED_DATE);

    /** The qualified column identifier for the {@link #flowCost} field. */
    public static final ColumnExp FLOW_COST_C =
        new ColumnExp(GameCatalogRecord.class, FLOW_COST);

    /** The qualified column identifier for the {@link #goldCost} field. */
    public static final ColumnExp GOLD_COST_C =
        new ColumnExp(GameCatalogRecord.class, GOLD_COST);

    /** The qualified column identifier for the {@link #rarity} field. */
    public static final ColumnExp RARITY_C =
        new ColumnExp(GameCatalogRecord.class, RARITY);

    /** The qualified column identifier for the {@link #purchases} field. */
    public static final ColumnExp PURCHASES_C =
        new ColumnExp(GameCatalogRecord.class, PURCHASES);

    /** The qualified column identifier for the {@link #returns} field. */
    public static final ColumnExp RETURNS_C =
        new ColumnExp(GameCatalogRecord.class, RETURNS);

    /** The qualified column identifier for the {@link #repriceCounter} field. */
    public static final ColumnExp REPRICE_COUNTER_C =
        new ColumnExp(GameCatalogRecord.class, REPRICE_COUNTER);
    // AUTO-GENERATED: FIELDS END

    public GameCatalogRecord ()
    {
        super();
    }

    protected GameCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #GameCatalogRecord}
     * with the supplied key values.
     */
    public static Key<GameCatalogRecord> getKey (int itemId)
    {
        return new Key<GameCatalogRecord>(
                GameCatalogRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
