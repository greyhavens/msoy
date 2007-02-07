//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.regex.Pattern;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Table;

import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.threerings.io.Streamable;

/**
 * Maps a tag's id to the tag itself.
 */
@Entity
@Table
public class TagNameRecord extends PersistentRecord
    implements Streamable
{
    public static final int SCHEMA_VERSION = 1;

    public static final String TAG_ID = "tagId";
    public static final ColumnExp TAG_ID_C = new ColumnExp(TagNameRecord.class, TAG_ID);
    public static final String TAG = "tag";
    public static final ColumnExp TAG_C = new ColumnExp(TagNameRecord.class, TAG);

    /** A regexp pattern to validate tags */
    public static final Pattern VALID_TAG = Pattern.compile("[a-z](_?[a-z0-9]){2,18}");

    /** The ID of this tag. */
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    public int tagId;

    /** The actual tag. */
    public String tag;
}
