//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;

/** Clone records for Furniture. */
@Entity
@Table
@TableGenerator(name="cloneId", pkColumnValue="FURNITURE_CLONE")
public class FurnitureCloneRecord extends CloneRecord<FurnitureRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #FurnitureCloneRecord}
     * with the supplied key values.
     */
    public static Key<FurnitureCloneRecord> getKey (int itemId)
    {
        return new Key<FurnitureCloneRecord>(
                FurnitureCloneRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
