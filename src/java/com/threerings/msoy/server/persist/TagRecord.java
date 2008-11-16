//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;

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

    /** The column identifier for the {@link #targetId} field. */
    public static final String TARGET_ID = "targetId";
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 3;

    /** The ID of the tag. */
    @Id
    public int tagId;

    /** The ID of the tagged target. */
    @Id
    public int targetId;
}
