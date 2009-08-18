//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import java.sql.Timestamp;

import com.samskivert.util.ByteEnum;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Records a user's action on Facebook for later funnel tuning. Currently only the server requires
 * this record, so there is no runtime version.
 * TODO: consider generalizing if and when we need to record actions on other external sites
 */
@Entity
public class FacebookActionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FacebookActionRecord> _R = FacebookActionRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp TYPE = colexp(_R, "type");
    public static final ColumnExp ID = colexp(_R, "id");
    public static final ColumnExp TIMESTAMP = colexp(_R, "timestamp");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    public static final int MAX_ID_LENGTH = 255;

    /**
     * Types of actions.
     */
    public enum Type
        implements ByteEnum
    {
        /** User published a trophy to their feed. */
        PUBLISHED_TROPHY(1);

        @Override // from ByteEnum
        public byte toByte ()
        {
            return _value;
        }

        Type (int value)
        {
            _value = (byte)(value);
        }

        protected byte _value;
    }

    /** Member that performed the action. */
    @Id public int memberId;

    /** Type of action performed. */
    @Id public Type type;

    /** Unique id of the action (e.g. "N:foo" for the foo trophy in game N). */
    @Column(length=MAX_ID_LENGTH)
    @Id public String id;

    /** When the action was performed. */
    public Timestamp timestamp;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FacebookActionRecord}
     * with the supplied key values.
     */
    public static Key<FacebookActionRecord> getKey (int memberId, FacebookActionRecord.Type type, String id)
    {
        return new Key<FacebookActionRecord>(
                FacebookActionRecord.class,
                new ColumnExp[] { MEMBER_ID, TYPE, ID },
                new Comparable[] { memberId, type, id });
    }
    // AUTO-GENERATED: METHODS END
}
