//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;

/**
 * Manages the persistent store of profile profile data.
 */
public class ProfileRepository extends DepotRepository
{
    public ProfileRepository (ConnectionProvider conprov)
    {
        super(conprov);
    }

    /**
     * Loads the profile record for the specified member. Returns null if no
     * record has been created for that member.
     */
    public ProfileRecord loadProfile (int memberId)
        throws PersistenceException
    {
        return load(ProfileRecord.class, memberId);
    }

    /**
     * Stores the supplied profile record in the database, overwriting an
     * previously stored profile data.
     */
    public void storeProfile (ProfileRecord record)
        throws PersistenceException
    {
        store(record);
    }
}
