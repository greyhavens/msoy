//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Maintains the persistent abuse data associated with a game.
 */
@Entity
public class GameAbuseRecord extends PersistentRecord
{
    public static final int SCHEMA_VERSION = 1;

    public static final String GAME_ID = "gameId";
    public static final ColumnExp GAME_ID_C =
        new ColumnExp(GameAbuseRecord.class, GAME_ID);
    public static final String ACCUM_MINUTES_SINCE_LAST_ASSESSMENT =
        "accumMinutesSinceLastAssessment";
    public static final ColumnExp ACCUM_MINUTES_SINCE_LAST_ASSESSMENT_C =
        new ColumnExp(GameAbuseRecord.class, ACCUM_MINUTES_SINCE_LAST_ASSESSMENT);
    public static final String ABUSE_FACTOR = "abuseFactor";
    public static final ColumnExp ABUSE_FACTOR_C =
        new ColumnExp(GameAbuseRecord.class, ABUSE_FACTOR);

    /** The id of the game we're tracking. */
    public int gameId;

    /** The accumulated number of player minutes since we last assessed the abuse factor. */
    public int accumMinutesSinceLastAssessment;

    /** The current abuse factor, from 0 to 255. */
    public int abuseFactor;
}
