//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.regex.Pattern;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;

import com.samskivert.depot.expression.ColumnExp;
import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.TagCodes;

/**
 * Maps a tag's id to the tag itself.
 */
@Entity
public class TagNameRecord extends PersistentRecord
    implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<TagNameRecord> _R = TagNameRecord.class;
    public static final ColumnExp TAG_ID = colexp(_R, "tagId");
    public static final ColumnExp TAG = colexp(_R, "tag");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** A regexp pattern to validate tags */
    public static final Pattern VALID_TAG = Pattern.compile(
        "([_a-z0-9]){" + TagCodes.MIN_TAG_LENGTH + "," + TagCodes.MAX_TAG_LENGTH + "}");

    /** Extracts the {@link #tag} from a {@link TagNameRecord}. */
    public static Function<TagNameRecord, String> TO_TAG = new Function<TagNameRecord, String>() {
        public String apply (TagNameRecord record) {
            return record.tag;
        }
    };

    /** The ID of this tag. */
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    public int tagId;

    /** The actual tag. */
    @Index(name="ixTag")
    public String tag;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link TagNameRecord}
     * with the supplied key values.
     */
    public static Key<TagNameRecord> getKey (int tagId)
    {
        return new Key<TagNameRecord>(
                TagNameRecord.class,
                new ColumnExp[] { TAG_ID },
                new Comparable[] { tagId });
    }
    // AUTO-GENERATED: METHODS END
}
