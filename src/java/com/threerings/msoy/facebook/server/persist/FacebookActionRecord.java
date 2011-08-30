//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Preconditions;

import com.samskivert.util.ByteEnum;
import com.samskivert.util.Tuple;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Records actions by or on a Facebook user for later funnel tuning. Currently only the server
 * requires this record, so there is no runtime version.
 * TODO: consider generalizing if and when we need to record actions on other external sites
 */
@Entity
public class FacebookActionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FacebookActionRecord> _R = FacebookActionRecord.class;
    public static final ColumnExp<Integer> APP_ID = colexp(_R, "appId");
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<FacebookActionRecord.Type> TYPE = colexp(_R, "type");
    public static final ColumnExp<String> ID = colexp(_R, "id");
    public static final ColumnExp<Timestamp> TIMESTAMP = colexp(_R, "timestamp");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    public static final int MAX_ID_LENGTH = 255;

    /**
     * Assembles the unique id for a trophy published action.
     */
    public static String getTrophyPublishedId (int gameId, String trophyIdent)
    {
        return gameId + ":" + trophyIdent;
    }

    /**
     * Creates an action representing a daily visit to the given application by the given member
     * of the given level for which the given amount of coins was rewarded.
     */
    public static FacebookActionRecord dailyVisit (
        int appId, int memberId, int coinsAwarded, int level)
    {
        // record the identifying values in slots and include time stamp in case 2 visits occur
        // with the same award and level
        long now = now();
        String id = now + ":" + coinsAwarded + ":" + level;
        return new FacebookActionRecord(appId, memberId, Type.DAILY_VISIT, id, now);
    }

    /**
     * Creates an action representing the publishing of the given trophy for the given game by the
     * given member and application.
     */
    public static FacebookActionRecord trophyPublished (
        int appId, int memberId, int gameId, String trophyIdent)
    {
        return new FacebookActionRecord(appId, memberId, Type.PUBLISHED_TROPHY,
            getTrophyPublishedId(gameId, trophyIdent), now());
    }

    /**
     * Creates an action representing the gathering of the given user's data for the given app.
     */
    public static FacebookActionRecord dataGathered (int appId, int memberId)
    {
        return new FacebookActionRecord(appId, memberId, Type.GATHERED_DATA, "server", now());
    }

    /**
     * Types of actions.
     */
    public enum Type
        implements ByteEnum
    {
        /** User published a trophy to their feed. */
        PUBLISHED_TROPHY(1),

        /** We gathered this user's demographic data. */
        GATHERED_DATA(2),

        /** The user visited the site. */
        DAILY_VISIT(3);

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

    /**
     * Creates a new action with the given fields and the current time.
     */
    public FacebookActionRecord (
        int appId, int memberId, FacebookActionRecord.Type type, String id, long timestamp)
    {
        this.appId = appId;
        this.memberId = memberId;
        this.type = type;
        this.id = id;
        this.timestamp = new Timestamp(timestamp);
    }

    /**
     * For deserialization.
     */
    public FacebookActionRecord ()
    {
    }

    /** Application on which the action was performed. */
    @Id public int appId;

    /** Member that performed the action. */
    @Id public int memberId;

    /** Type of action performed. */
    @Id public Type type;

    /** Unique id of the action (e.g. "N:foo" for the foo trophy in game N). */
    @Column(length=MAX_ID_LENGTH)
    @Id public String id;

    /** When the action was performed. */
    public Timestamp timestamp;

    /**
     * For DAILY_VISIT actions, extracts the coins awarded for the visit and the level of the user
     * at the time.
     */
    public Tuple<Integer, Integer> extractCoinAwardAndLevel ()
    {
        Preconditions.checkArgument(type == Type.DAILY_VISIT);
        String[] values = id.split(":");
        return Tuple.newTuple(Integer.parseInt(values[1]), Integer.parseInt(values[2]));
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FacebookActionRecord}
     * with the supplied key values.
     */
    public static Key<FacebookActionRecord> getKey (int appId, int memberId, FacebookActionRecord.Type type, String id)
    {
        return newKey(_R, appId, memberId, type, id);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(APP_ID, MEMBER_ID, TYPE, ID); }
    // AUTO-GENERATED: METHODS END

    protected static long now ()
    {
        return System.currentTimeMillis();
    }
}
