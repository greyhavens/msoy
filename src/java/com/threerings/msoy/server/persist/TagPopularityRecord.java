//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;

import com.threerings.io.Streamable;

/**
 * Maps a tag to a count of the targets that reference it.
 */
@Computed(shadowOf=TagNameRecord.class)
@Entity
public class TagPopularityRecord extends PersistentRecord
    implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #tagId} field. */
    public static final String TAG_ID = "tagId";

    /** The qualified column identifier for the {@link #tagId} field. */
    public static final ColumnExp TAG_ID_C =
        new ColumnExp(TagPopularityRecord.class, TAG_ID);

    /** The column identifier for the {@link #tag} field. */
    public static final String TAG = "tag";

    /** The qualified column identifier for the {@link #tag} field. */
    public static final ColumnExp TAG_C =
        new ColumnExp(TagPopularityRecord.class, TAG);

    /** The column identifier for the {@link #count} field. */
    public static final String COUNT = "count";

    /** The qualified column identifier for the {@link #count} field. */
    public static final ColumnExp COUNT_C =
        new ColumnExp(TagPopularityRecord.class, COUNT);
    // AUTO-GENERATED: FIELDS END

    /** The ID of this tag. */
    public int tagId;

    /** The actual tag. */
    public String tag;

    /** The number of target that reference this tag. */
    @Computed(fieldDefinition="count(*)")
    public int count;
}
