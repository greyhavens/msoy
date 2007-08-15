//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;

import com.threerings.io.Streamable;

/**
 * Represents which tags have been added to which targets.
 */
@Entity(indices={
  @Index(name="ixTarget", fields={ TagRecord.TARGET_ID })
})
public abstract class TagRecord extends PersistentRecord
    implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #tagId} field. */
    public static final String TAG_ID = "tagId";

    /** The qualified column identifier for the {@link #tagId} field. */
    public static final ColumnExp TAG_ID_C =
        new ColumnExp(TagRecord.class, TAG_ID);

    /** The column identifier for the {@link #targetId} field. */
    public static final String TARGET_ID = "targetId";

    /** The qualified column identifier for the {@link #targetId} field. */
    public static final ColumnExp TARGET_ID_C =
        new ColumnExp(TagRecord.class, TARGET_ID);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 3;

    /** The ID of the tag. */
    @Id
    public int tagId;

    /** The ID of the tagged target. */
    @Id
    public int targetId;
}
