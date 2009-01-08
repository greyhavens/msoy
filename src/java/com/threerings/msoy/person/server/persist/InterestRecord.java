//
// $Id$

package com.threerings.msoy.person.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.FullTextIndex;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;
import com.threerings.msoy.person.gwt.Interest;

@Entity(fullTextIndices={
    @FullTextIndex(name=InterestRecord.FTS_INTERESTS, fields={ "interests" })
})
public class InterestRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<InterestRecord> _R = InterestRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp TYPE = colexp(_R, "type");
    public static final ColumnExp INTERESTS = colexp(_R, "interests");
    // AUTO-GENERATED: FIELDS END

    /** The name of the full text index on the {@link #interests} field. */
    public static final String FTS_INTERESTS = "I";

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The unique id of the member. */
    @Id public int memberId;

    /** The type of interest in question. @see Interest */
    @Id public int type;

    /** A raw string provided by the user. */
    @Column(length=Interest.MAX_INTEREST_LENGTH)
    public String interests;

    /** Converts this persistent record to a runtime record. */
    public Interest toRecord ()
    {
        Interest record = new Interest();
        record.type = type;
        record.interests = interests;

        return record;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link InterestRecord}
     * with the supplied key values.
     */
    public static Key<InterestRecord> getKey (int memberId, int type)
    {
        return new Key<InterestRecord>(
                InterestRecord.class,
                new ColumnExp[] { MEMBER_ID, TYPE },
                new Comparable[] { memberId, type });
    }
    // AUTO-GENERATED: METHODS END
}
