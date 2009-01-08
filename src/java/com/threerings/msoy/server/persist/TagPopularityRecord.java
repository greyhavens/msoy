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
    public static final Class<TagPopularityRecord> _R = TagPopularityRecord.class;
    public static final ColumnExp TAG_ID = colexp(_R, "tagId");
    public static final ColumnExp TAG = colexp(_R, "tag");
    public static final ColumnExp COUNT = colexp(_R, "count");
    // AUTO-GENERATED: FIELDS END

    /** The ID of this tag. */
    public int tagId;

    /** The actual tag. */
    public String tag;

    /** The number of target that reference this tag. */
    @Computed(fieldDefinition="count(*)")
    public int count;
}
