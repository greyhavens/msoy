//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;

import com.threerings.io.Streamable;

/**
 * Represents which tags have been added to which targets.
 */
@Entity
public abstract class TagRecord extends PersistentRecord
    implements Streamable
{
    public static final int SCHEMA_VERSION = 2;

    public static final String TAG_ID = "tagId";
    public static final String TARGET_ID = "targetId";

    /** The ID of the tag. */
    @Id
    public int tagId;

     /** The ID of the tagged target. */
    @Id
    public int targetId;
}
