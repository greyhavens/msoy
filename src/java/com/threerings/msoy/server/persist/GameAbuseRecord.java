//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Maintains the persistent abuse data associated with a game.
 */
@Entity
public class GameAbuseRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #gameId} field. */
    public static final String GAME_ID = "gameId";

    /** The qualified column identifier for the {@link #gameId} field. */
    public static final ColumnExp GAME_ID_C =
        new ColumnExp(GameAbuseRecord.class, GAME_ID);

    /** The column identifier for the {@link #accumMinutes} field. */
    public static final String ACCUM_MINUTES = "accumMinutes";

    /** The qualified column identifier for the {@link #accumMinutes} field. */
    public static final ColumnExp ACCUM_MINUTES_C =
        new ColumnExp(GameAbuseRecord.class, ACCUM_MINUTES);

    /** The column identifier for the {@link #abuseFactor} field. */
    public static final String ABUSE_FACTOR = "abuseFactor";

    /** The qualified column identifier for the {@link #abuseFactor} field. */
    public static final ColumnExp ABUSE_FACTOR_C =
        new ColumnExp(GameAbuseRecord.class, ABUSE_FACTOR);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** The id of the game we're tracking. */
    @Id
    public int gameId;

    /** The accumulated number of player minutes since we last assessed the abuse factor. */
    public int accumMinutes;

    /** The current abuse factor, from 0 to 255. */
    public int abuseFactor;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #GameAbuseRecord}
     * with the supplied key values.
     */
    public static Key<GameAbuseRecord> getKey (int gameId)
    {
        return new Key<GameAbuseRecord>(
                GameAbuseRecord.class,
                new String[] { GAME_ID },
                new Comparable[] { gameId });
    }
    // AUTO-GENERATED: METHODS END
}
