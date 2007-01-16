//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.io.Streamable;

/**
 * Maps a tag to a count of the items that reference it.
 */
@Computed
@Entity
public class TagPopularityRecord extends PersistentRecord
    implements Streamable
{
    public static final String TAG_ID = "tagId";
    public static final String TAG = "tag";
    public static final String COUNT = "count";

    /** The ID of this tag. */
    public int tagId;

    /** The actual tag. */
    public String tag;
    
    /** The number of item that reference this tag. */
    public int count;
}
