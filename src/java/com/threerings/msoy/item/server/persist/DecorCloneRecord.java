//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;

/** Clone records for Decor. */
@Entity
@Table
@TableGenerator(name="cloneId", pkColumnValue="DECOR_CLONE")
public class DecorCloneRecord extends CloneRecord<DecorRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #DecorCloneRecord}
     * with the supplied key values.
     */
    public static Key<DecorCloneRecord> getKey (int itemId)
    {
        return new Key<DecorCloneRecord>(
                DecorCloneRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
