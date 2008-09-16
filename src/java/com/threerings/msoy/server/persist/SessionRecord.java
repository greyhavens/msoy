//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations

import java.sql.Date;

/**
 * Contains a mapping from a session token to a member id. Used to persist an
 * authenticated session for some period of time.
 */
@Entity
public class SessionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #token} field. */
    public static final String TOKEN = "token";

    /** The qualified column identifier for the {@link #token} field. */
    public static final ColumnExp TOKEN_C =
        new ColumnExp(SessionRecord.class, TOKEN);

    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(SessionRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #expires} field. */
    public static final String EXPIRES = "expires";

    /** The qualified column identifier for the {@link #expires} field. */
    public static final ColumnExp EXPIRES_C =
        new ColumnExp(SessionRecord.class, EXPIRES);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The unique session identifier. */
    @Id
    public String token;

    /** The id of the member bound to this session. */
    @Column(unique=true)
    public int memberId;

    /** The time at which this session will be expired. */
    public Date expires;

    /** Used when unserializing. */
    public SessionRecord ()
    {
    }

    /** Used to create a "key" for doing database fiddling. */
    public SessionRecord (String sessionToken)
    {
        token = sessionToken;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SessionRecord}
     * with the supplied key values.
     */
    public static Key<SessionRecord> getKey (String token)
    {
        return new Key<SessionRecord>(
                SessionRecord.class,
                new String[] { TOKEN },
                new Comparable[] { token });
    }
    // AUTO-GENERATED: METHODS END
}
