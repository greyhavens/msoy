//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;

/** Clone records for Pet. */
@Entity
@Table
@TableGenerator(name="cloneId", pkColumnValue="PET_CLONE")
public class PetCloneRecord extends CloneRecord<PetRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #PetCloneRecord}
     * with the supplied key values.
     */
    public static Key<PetCloneRecord> getKey (int itemId)
    {
        return new Key<PetCloneRecord>(
                PetCloneRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
