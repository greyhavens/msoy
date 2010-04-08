//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.facebook.gwt.KontagentInfo;

/**
 * Describes the parameters necessary for sending Facebook usage data messages to Kontagent.
 */
@Entity
public class KontagentInfoRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<KontagentInfoRecord> _R = KontagentInfoRecord.class;
    public static final ColumnExp APP_ID = colexp(_R, "appId");
    public static final ColumnExp API_KEY = colexp(_R, "apiKey");
    public static final ColumnExp API_SECRET = colexp(_R, "apiSecret");
    // AUTO-GENERATED: FIELDS END

    /** Tracks code changes that require table alterations. */
    public static final int SCHEMA_VERSION = 1;

    /** The application associated with Kontagent. */
    @Id public int appId;

    /** The api key for accessing the Kontagent application upload folder. */
    @Column(length=KontagentInfo.KEY_LENGTH)
    public String apiKey;

    /** The secret for hashing Kontagent message parameters. */
    @Column(length=KontagentInfo.SECRET_LENGTH)
    public String apiSecret;

    /**
     * Creates a new record for deserializing from the database.
     */
    public KontagentInfoRecord ()
    {
    }

    /**
     * Creates a new record with the given application and runtime information.
     */
    public KontagentInfoRecord (int appId, KontagentInfo kinfo)
    {
        this.appId = appId;
        this.apiKey = kinfo.apiKey;
        this.apiSecret = kinfo.apiSecret;
    }

    /**
     * Converts this record to a runtime POJO.
     */
    public KontagentInfo toKontagentInfo ()
    {
        return new KontagentInfo(apiKey, apiSecret);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link KontagentInfoRecord}
     * with the supplied key values.
     */
    public static Key<KontagentInfoRecord> getKey (int appId)
    {
        return newKey(_R, appId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(APP_ID); }
    // AUTO-GENERATED: METHODS END
}
