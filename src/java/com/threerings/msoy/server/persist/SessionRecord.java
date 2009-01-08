//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.*; // for Depot annotations

import java.sql.Date;

/**
 * Contains a mapping from a session token to a member id. Used to persist an
 * authenticated session for some period of time.
 */
@Entity
public class SessionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SessionRecord> _R = SessionRecord.class;
    public static final ColumnExp TOKEN = colexp(_R, "token");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp EXPIRES = colexp(_R, "expires");
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
                new ColumnExp[] { TOKEN },
                new Comparable[] { token });
    }
    // AUTO-GENERATED: METHODS END
}
