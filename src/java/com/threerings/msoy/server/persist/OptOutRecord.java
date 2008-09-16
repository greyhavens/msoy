//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.samskivert.util.StringUtil;

/**
 * Contains persistent data stored for email addresses that have opted-out of email from Whirled.
 */
@Entity
public class OptOutRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #email} field. */
    public static final String EMAIL = "email";

    /** The qualified column identifier for the {@link #email} field. */
    public static final ColumnExp EMAIL_C =
        new ColumnExp(OptOutRecord.class, EMAIL);
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
                new String[] { EMAIL },
                new Comparable[] { email });
    }
    // AUTO-GENERATED: METHODS END
}
