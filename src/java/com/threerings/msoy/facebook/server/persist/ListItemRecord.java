//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Represents an element in a persistent ordered list. The elements referred to can be anything
 * identifiable by a limited length string.
 */
public class ListItemRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ListItemRecord> _R = ListItemRecord.class;
    public static final ColumnExp<String> LIST_ID = colexp(_R, "listId");
    public static final ColumnExp<Integer> INDEX = colexp(_R, "index");
    public static final ColumnExp<String> ID = colexp(_R, "id");
    // AUTO-GENERATED: FIELDS END

    /** Increase whenever any change requires a table alteration to work. */
    public static final int SCHEMA_VERSION = 1;

    /** The maximum length of the list id. */
    public static final int MAX_LIST_ID_LENGTH = 32;

    /** The maximum length of the ids of the referenced items. */
    public static final int MAX_REF_ID_LENGTH = 32;

    /** Indicates the list to which this element belongs. */
    @Column(length=MAX_LIST_ID_LENGTH)
    @Id public String listId;

    /** The position of this element within the list. */
    @Id public int index;

    /** The id of the referenced object. Any object can be referred to as long as its id fits. */
    @Column(length=MAX_REF_ID_LENGTH)
    public String id;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ListItemRecord}
     * with the supplied key values.
     */
    public static Key<ListItemRecord> getKey (String listId, int index)
    {
        return newKey(_R, listId, index);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(LIST_ID, INDEX); }
    // AUTO-GENERATED: METHODS END
}
