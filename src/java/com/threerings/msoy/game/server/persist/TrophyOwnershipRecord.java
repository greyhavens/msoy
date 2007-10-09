//
// $Id$

package com.threerings.msoy.game.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;

/**
 * Used to determine which trophies are owned by a player for a particular game.
 */
@Computed(shadowOf=TrophyRecord.class)
@Entity
public class TrophyOwnershipRecord extends PersistentRecord
{
    /** The identifier of a trophy owned by a player. */
    public String ident;
}
