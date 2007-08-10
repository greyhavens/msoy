//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.TableGenerator;

/** Clone records for Photos. */
@TableGenerator(name="cloneId", pkColumnValue="PHOTO_CLONE")
public class PhotoCloneRecord extends CloneRecord<PhotoRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #PhotoCloneRecord}
     * with the supplied key values.
     */
    public static Key<PhotoCloneRecord> getKey (int itemId)
    {
        return new Key<PhotoCloneRecord>(
                PhotoCloneRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
