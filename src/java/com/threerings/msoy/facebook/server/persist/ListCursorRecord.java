//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Persistent reference to a position in a persistent list (of {@link ListItemRecord}).
 */
public class ListCursorRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ListCursorRecord> _R = ListCursorRecord.class;
    public static final ColumnExp LIST_ID = colexp(_R, "listId");
    public static final ColumnExp CURSOR_ID = colexp(_R, "cursorId");
    public static final ColumnExp INDEX = colexp(_R, "index");
    // AUTO-GENERATED: FIELDS END

    /** Increase whenever any change requires a table alteration to work. */
    public static final int SCHEMA_VERSION = 1;

    /** Maximum length of cursor ids. */
    public static final int MAX_CURSOR_ID_LENGTH = 32;

    @Column(length=ListItemRecord.MAX_LIST_ID_LENGTH)
    @Id public String listId;

    @Column(length=MAX_CURSOR_ID_LENGTH)
    @Id public String cursorId;

    public int index;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ListCursorRecord}
     * with the supplied key values.
     */
    public static Key<ListCursorRecord> getKey (String listId, String cursorId)
    {
        return new Key<ListCursorRecord>(
                ListCursorRecord.class,
                new ColumnExp[] { LIST_ID, CURSOR_ID },
                new Comparable[] { listId, cursorId });
    }
    // AUTO-GENERATED: METHODS END
}
