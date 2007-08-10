//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.TableGenerator;

/** Clone records for Toy. */
@TableGenerator(name="cloneId", pkColumnValue="FURNITURE_CLONE")
public class ToyCloneRecord extends CloneRecord<ToyRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #ToyCloneRecord}
     * with the supplied key values.
     */
    public static Key<ToyCloneRecord> getKey (int itemId)
    {
        return new Key<ToyCloneRecord>(
                ToyCloneRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
