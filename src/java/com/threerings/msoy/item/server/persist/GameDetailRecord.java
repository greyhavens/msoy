//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.util.StringUtil;

/**
 * Contains details on a single game "title" which may span multiple versions and therefore
 * multiple item ids. Some day additional information like screenshots and instructions may also be
 * contained in the detail record, but for now it serves simply to track a unique game identifier
 * that is shared by all versions of the same game.
 */
public class GameDetailRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #gameId} field. */
    public static final String GAME_ID = "gameId";

    /** The qualified column identifier for the {@link #gameId} field. */
    public static final ColumnExp GAME_ID_C =
        new ColumnExp(GameDetailRecord.class, GAME_ID);

    /** The column identifier for the {@link #listedItemId} field. */
    public static final String LISTED_ITEM_ID = "listedItemId";

    /** The qualified column identifier for the {@link #listedItemId} field. */
    public static final ColumnExp LISTED_ITEM_ID_C =
        new ColumnExp(GameDetailRecord.class, LISTED_ITEM_ID);

    /** The column identifier for the {@link #sourceItemId} field. */
    public static final String SOURCE_ITEM_ID = "sourceItemId";

    /** The qualified column identifier for the {@link #sourceItemId} field. */
    public static final ColumnExp SOURCE_ITEM_ID_C =
        new ColumnExp(GameDetailRecord.class, SOURCE_ITEM_ID);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The unique identifier for this game. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int gameId;

    /** The canonical game item for this game, which has been listed in the catalog. */
    public int listedItemId;

    /** The mutable item which is edited by the developer(s) working on this game. */
    public int sourceItemId;

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #GameDetailRecord}
     * with the supplied key values.
     */
    public static Key<GameDetailRecord> getKey (int gameId)
    {
        return new Key<GameDetailRecord>(
                GameDetailRecord.class,
                new String[] { GAME_ID },
                new Comparable[] { gameId });
    }
    // AUTO-GENERATED: METHODS END
}
