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
    public static final Class<TrophyOwnershipRecord> _R = TrophyOwnershipRecord.class;
    public static final ColumnExp IDENT = colexp(_R, "ident");
    // AUTO-GENERATED: FIELDS END

    /** The identifier of a trophy owned by a player. */
    public String ident;
}
