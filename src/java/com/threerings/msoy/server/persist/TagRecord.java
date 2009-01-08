//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;

import com.threerings.io.Streamable;

/**
 * Represents which tags have been added to which targets.
 */
@Entity
public abstract class TagRecord extends PersistentRecord
    implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<TagRecord> _R = TagRecord.class;
    public static final ColumnExp TAG_ID = colexp(_R, "tagId");
    public static final ColumnExp TARGET_ID = colexp(_R, "targetId");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 3;

    /** The ID of the tag. */
    @Id
    public int tagId;

    /** The ID of the tagged target. */
    @Id @Index(name="ixTarget")
    public int targetId;
}
