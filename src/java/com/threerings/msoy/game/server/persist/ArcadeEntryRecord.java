//
// $Id$

package com.threerings.msoy.game.server.persist;

import com.google.common.base.Function;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.game.gwt.ArcadeData;

/**
 * Holds information for a game that we want to feature on the games landing page.
 */
@Entity
public class ArcadeEntryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ArcadeEntryRecord> _R = ArcadeEntryRecord.class;
    public static final ColumnExp PORTAL = colexp(_R, "portal");
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp ORDER = colexp(_R, "order");
    public static final ColumnExp FEATURED = colexp(_R, "featured");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    public static Function<ArcadeEntryRecord, Integer> TO_GAME_ID =
        new Function<ArcadeEntryRecord, Integer>() {
            public Integer apply (ArcadeEntryRecord entry) {
                return entry.gameId;
            }
        };

    /** The portal this record is for (we support multiple games front pages). */
    @Id public ArcadeData.Portal portal;

    /** ID of the game that is a top one. */
    @Id public int gameId;

    /** Where the game appears in lists. */
    public int order;

    /** If the game is featured, or just a top game. */
    public boolean featured;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ArcadeEntryRecord}
     * with the supplied key values.
     */
    public static Key<ArcadeEntryRecord> getKey (ArcadeData.Portal portal, int gameId)
    {
        return newKey(_R, portal, gameId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(PORTAL, GAME_ID); }
    // AUTO-GENERATED: METHODS END
}
