//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Extends the basic samskivert user record with special Three Rings
 * business.
 */
@Entity(name="USER_IDENTS", indices={
    @Index(name="ixUser", columns={ UserIdentRecord.USER_ID })
})
public class UserIdentRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #userId} field. */
    public static final String USER_ID = "userId";

    /** The qualified column identifier for the {@link #userId} field. */
    public static final ColumnExp USER_ID_C =
        new ColumnExp(UserIdentRecord.class, USER_ID);

    /** The column identifier for the {@link #machIdent} field. */
    public static final String MACH_IDENT = "machIdent";

    /** The qualified column identifier for the {@link #machIdent} field. */
    public static final ColumnExp MACH_IDENT_C =
        new ColumnExp(UserIdentRecord.class, MACH_IDENT);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** The id of the user in question. */
    @Id
    @Column(name="USER_ID")
    public int userId;

    /** A 'unique' id for a specific machine we have seen the user come from. */
    @Id
    @Column(name="MACH_IDENT")
    public String machIdent;

    public UserIdentRecord ()
    {
        super();
    }

    public UserIdentRecord (int userId, String machIdent)
    {
        super();
        this.userId = userId;
        this.machIdent = machIdent;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #UserIdentRecord}
     * with the supplied key values.
     */
    public static Key<UserIdentRecord> getKey (int userId, String machIdent)
    {
        return new Key<UserIdentRecord>(
                UserIdentRecord.class,
                new String[] { USER_ID, MACH_IDENT },
                new Comparable[] { userId, machIdent });
    }
    // AUTO-GENERATED: METHODS END
}
