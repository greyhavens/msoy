//
// $Id$

package com.threerings.msoy.avrg.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Maintains information associated with a given member for a given game.
 */
@Entity
public class GameStateRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #gameId} field. */
    public static final String GAME_ID = "gameId";

    /** The qualified column identifier for the {@link #gameId} field. */
    public static final ColumnExp GAME_ID_C =
        new ColumnExp(GameStateRecord.class, GAME_ID);

    /** The column identifier for the {@link #datumKey} field. */
    public static final String DATUM_KEY = "datumKey";

    /** The qualified column identifier for the {@link #datumKey} field. */
    public static final ColumnExp DATUM_KEY_C =
        new ColumnExp(GameStateRecord.class, DATUM_KEY);

    /** The column identifier for the {@link #datumValue} field. */
    public static final String DATUM_VALUE = "datumValue";

    /** The qualified column identifier for the {@link #datumValue} field. */
    public static final ColumnExp DATUM_VALUE_C =
        new ColumnExp(GameStateRecord.class, DATUM_VALUE);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    @Id
    public int gameId;

    @Id
    /** The key that identifies this memory datum. */
    public String datumKey;

    /** A serialized representation of this datum's value. */
    @Column(length=4096)
    public byte[] datumValue;

    /** Used when loading instances from the repository. */
    public GameStateRecord ()
    {
    }

    /**
     * Creates a memory record from the supplied memory information.
     */
    public GameStateRecord (int gameId, String key, byte[] data)
    {
        this.gameId = gameId;
        this.datumKey = key;
        this.datumValue = data;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #GameStateRecord}
     * with the supplied key values.
     */
    public static Key<GameStateRecord> getKey (int gameId, String datumKey)
    {
        return new Key<GameStateRecord>(
                GameStateRecord.class,
                new String[] { GAME_ID, DATUM_KEY },
                new Comparable[] { gameId, datumKey });
    }
    // AUTO-GENERATED: METHODS END
}
