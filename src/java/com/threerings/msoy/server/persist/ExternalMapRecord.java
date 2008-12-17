//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.web.gwt.ExternalAuther;

/**
 * Maps our member accounts to external user ids. Used to integrate with external services like
 * Facebook.
 */
@Entity(indices={
    @Index(name="ixMemberId", fields={ ExternalMapRecord.MEMBER_ID })
})
public class ExternalMapRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #partnerId} field. */
    public static final String PARTNER_ID = "partnerId";

    /** The qualified column identifier for the {@link #partnerId} field. */
    public static final ColumnExp PARTNER_ID_C =
        new ColumnExp(ExternalMapRecord.class, PARTNER_ID);

    /** The column identifier for the {@link #externalId} field. */
    public static final String EXTERNAL_ID = "externalId";

    /** The qualified column identifier for the {@link #externalId} field. */
    public static final ColumnExp EXTERNAL_ID_C =
        new ColumnExp(ExternalMapRecord.class, EXTERNAL_ID);

    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(ExternalMapRecord.class, MEMBER_ID);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** The partner that maintains the external user id. See {@link ExternalAuther}. */
    @Id public int partnerId;

    /** The external user identifier. Might be numberic, but we'll store it as a string in case we
     * one day want to support an external site that uses string identifiers. */
    @Id public String externalId;

    /** The id of the Whirled account associated with the specified external account. */
    public int memberId;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ExternalMapRecord}
     * with the supplied key values.
     */
    public static Key<ExternalMapRecord> getKey (int partnerId, String externalId)
    {
        return new Key<ExternalMapRecord>(
                ExternalMapRecord.class,
                new String[] { PARTNER_ID, EXTERNAL_ID },
                new Comparable[] { partnerId, externalId });
    }
    // AUTO-GENERATED: METHODS END
}
