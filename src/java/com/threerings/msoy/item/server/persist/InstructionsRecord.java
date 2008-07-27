//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.game.gwt.GameDetail;

/**
 * Contains a game's instructions.
 */
@Entity
public class InstructionsRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #gameId} field. */
    public static final String GAME_ID = "gameId";

    /** The qualified column identifier for the {@link #gameId} field. */
    public static final ColumnExp GAME_ID_C =
        new ColumnExp(InstructionsRecord.class, GAME_ID);

    /** The column identifier for the {@link #instructions} field. */
    public static final String INSTRUCTIONS = "instructions";

    /** The qualified column identifier for the {@link #instructions} field. */
    public static final ColumnExp INSTRUCTIONS_C =
        new ColumnExp(InstructionsRecord.class, INSTRUCTIONS);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The game for which we track instructions. */
    @Id public int gameId;

    /** The creator supplied instructions for this game. */
    @Column(length=GameDetail.MAX_INSTRUCTIONS_LENGTH)
    public String instructions;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #InstructionsRecord}
     * with the supplied key values.
     */
    public static Key<InstructionsRecord> getKey (int gameId)
    {
        return new Key<InstructionsRecord>(
                InstructionsRecord.class,
                new String[] { GAME_ID },
                new Comparable[] { gameId });
    }
    // AUTO-GENERATED: METHODS END
}
