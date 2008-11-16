//
// $Id$

package com.threerings.msoy.game.server.persist;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Used to determine which trophies are owned by a player for a particular game.
 */
@Computed(shadowOf=TrophyRecord.class)
@Entity
public class TrophyOwnershipRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #ident} field. */
    public static final String IDENT = "ident";

    /** The qualified column identifier for the {@link #ident} field. */
    public static final ColumnExp IDENT_C =
        new ColumnExp(TrophyOwnershipRecord.class, IDENT);
    // AUTO-GENERATED: FIELDS END

    /** The identifier of a trophy owned by a player. */
    public String ident;
}
