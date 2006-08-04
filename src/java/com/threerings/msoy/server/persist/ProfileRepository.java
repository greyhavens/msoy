//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Connection;
import java.sql.SQLException;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.Table;

/**
 * Manages the persistent store of profile profile data.
 */
public class ProfileRepository extends JORARepository
{
    /** The database identifier used when establishing a database
     * connection. This value being <code>profiledb</code>. */
    public static final String PROFILE_DB_IDENT = "profiledb";

    /**
     * Constructs a new profile repository with the specified connection
     * provider.
     *
     * @param conprov the connection provider via which we will obtain our
     * database connection.
     */
    public ProfileRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, PROFILE_DB_IDENT);
    }

    /**
     * Loads the profile record for the specified member. Returns null if no
     * record has been created for that member.
     */
    public ProfileRecord loadProfile (int memberId)
        throws PersistenceException
    {
        return load(_ptable, "where MEMBER_ID = " + memberId);
    }

    /**
     * Stores the supplied profile record in the database, overwriting an
     * previously stored profile data.
     */
    public void storeProfile (ProfileRecord record)
        throws PersistenceException
    {
        store(_ptable, record);
    }

    @Override // documentation inherited
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "PROFILES", new String[] {
            "MEMBER_ID integer not null",
            "HOME_PAGE_URL varchar(255) not null",
            "HEADLINE varchar(255) not null",
            "IS_MALE tinyint not null",
            "BIRTHDAY date not null",
            "LOCATION varchar(255) not null",
            "primary key (MEMBER_ID)",
        }, "");
    }

    @Override // documentation inherited
    protected void createTables ()
    {
	_ptable = new Table<ProfileRecord>(
            ProfileRecord.class, "PROFILES", "MEMBER_ID", true);
    }

    protected Table<ProfileRecord> _ptable;
}
