//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

import com.samskivert.util.StringUtil;

/**
 * Contains persistent data stored for email addresses that have opted-out of email from Whirled.
 */
@Entity
public class OptOutRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<OptOutRecord> _R = OptOutRecord.class;
    public static final ColumnExp EMAIL = colexp(_R, "email");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    @Id
    public String email;

    /**
     * Blank constructor for records from the database.
     */
    public OptOutRecord ()
    {
    }

    /**
     * Convenience constructor.
     */
    public OptOutRecord (String email)
    {
        this.email = email;
    }

    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link OptOutRecord}
     * with the supplied key values.
     */
    public static Key<OptOutRecord> getKey (String email)
    {
        return new Key<OptOutRecord>(
                OptOutRecord.class,
                new ColumnExp[] { EMAIL },
                new Comparable[] { email });
    }
    // AUTO-GENERATED: METHODS END
}
