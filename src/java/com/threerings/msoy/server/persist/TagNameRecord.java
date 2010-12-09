//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.regex.Pattern;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.StringFuncs;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.clause.OrderBy.Order;

import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.util.Tuple;
import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.TagCodes;

/**
 * Maps a tag's id to the tag itself.
 */
@Entity(indices=@Index(name="ixLowerTag"))
public class TagNameRecord extends PersistentRecord
    implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<TagNameRecord> _R = TagNameRecord.class;
    public static final ColumnExp<Integer> TAG_ID = colexp(_R, "tagId");
    public static final ColumnExp<String> TAG = colexp(_R, "tag");
    // AUTO-GENERATED: FIELDS END

    /** Defines the index on {@link #tag} converted to lower case. */
    public static Tuple<SQLExpression<?>, Order> ixLowerTag ()
    {
        return new Tuple<SQLExpression<?>, Order>(StringFuncs.lower(TagNameRecord.TAG), Order.ASC);
    }

    public static final int SCHEMA_VERSION = 3;

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
    public String tag;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link TagNameRecord}
     * with the supplied key values.
     */
    public static Key<TagNameRecord> getKey (int tagId)
    {
        return newKey(_R, tagId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(TAG_ID); }
    // AUTO-GENERATED: METHODS END
}
