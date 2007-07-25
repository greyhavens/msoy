//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;

/** Clone records for Videos. */
@Entity
@Table
@TableGenerator(name="cloneId", pkColumnValue="VIDEO_CLONE")
public class VideoCloneRecord extends CloneRecord<VideoRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #VideoCloneRecord}
     * with the supplied key values.
     */
    public static Key<VideoCloneRecord> getKey (int itemId)
    {
        return new Key<VideoCloneRecord>(
                VideoCloneRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
