//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.TableGenerator;

/** Clone records for Documents. */
@TableGenerator(name="cloneId", pkColumnValue="DOCUMENT_CLONE")
public class DocumentCloneRecord extends CloneRecord<DocumentRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #DocumentCloneRecord}
     * with the supplied key values.
     */
    public static Key<DocumentCloneRecord> getKey (int itemId)
    {
        return new Key<DocumentCloneRecord>(
                DocumentCloneRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
