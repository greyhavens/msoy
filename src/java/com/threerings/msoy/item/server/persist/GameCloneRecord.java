//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.TableGenerator;

/** Clone records for Games. */
@TableGenerator(name="cloneId", pkColumnValue="GAME_CLONE")
public class GameCloneRecord extends CloneRecord<GameRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #GameCloneRecord}
     * with the supplied key values.
     */
    public static Key<GameCloneRecord> getKey (int itemId)
    {
        return new Key<GameCloneRecord>(
                GameCloneRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
