//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Retains information about a historical user registration, Depot version.
 */
@Entity(name="history")
public class HistoricalUserRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #userId} field. */
    public static final String USER_ID = "userId";

    /** The qualified column identifier for the {@link #userId} field. */
    public static final ColumnExp USER_ID_C =
        new ColumnExp(HistoricalUserRecord.class, USER_ID);

    /** The column identifier for the {@link #username} field. */
    public static final String USERNAME = "username";

    /** The qualified column identifier for the {@link #username} field. */
    public static final ColumnExp USERNAME_C =
        new ColumnExp(HistoricalUserRecord.class, USERNAME);

    /** The column identifier for the {@link #created} field. */
    public static final String CREATED = "created";

    /** The qualified column identifier for the {@link #created} field. */
    public static final ColumnExp CREATED_C =
        new ColumnExp(HistoricalUserRecord.class, CREATED);

    /** The column identifier for the {@link #siteId} field. */
    public static final String SITE_ID = "siteId";

    /** The qualified column identifier for the {@link #siteId} field. */
    public static final ColumnExp SITE_ID_C =
        new ColumnExp(HistoricalUserRecord.class, SITE_ID);
    // AUTO-GENERATED: FIELDS END

    /** The user's assigned integer userid. */
    public int userId;

    /** The user's chosen username. */
    @Column(length=24)
    public String username;

    /** The date this record was created. */
    public Date created;

    /** The affiliate site with which this user is associated. */
    public int siteId;


    /** An empty constructor for deserialization. */
    public HistoricalUserRecord ()
    {
        super();
    }

    /** A constructor that populates this record. */
    public HistoricalUserRecord (int userId, String username, Date created, int siteId)
    {
        super();
        this.userId = userId;
        this.username = username;
        this.created = created;
        this.siteId = siteId;
    }
}
