//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.util.Collection;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.IntListUtil;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.In;

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
     * Loads the profile record for the specified member. Returns null if no record has been
     * created for that member.
     */
    public ProfileRecord loadProfile (int memberId)
        throws PersistenceException
    {
        return load(ProfileRecord.class, memberId);
    }

    /**
     * Loads the profile photos for all of the specified members.
     */
    public Collection<ProfilePhotoRecord> loadProfilePhotos (int[] memberIds)
        throws PersistenceException
    {
        return findAll(ProfilePhotoRecord.class,
                       new FromOverride(ProfileRecord.class),
                       new Where(new In(ProfileRecord.MEMBER_ID_C, IntListUtil.box(memberIds))),
                       new FieldOverride(ProfilePhotoRecord.MEMBER_ID, ProfileRecord.MEMBER_ID_C),
                       new FieldOverride(ProfilePhotoRecord.PHOTO_HASH, ProfileRecord.PHOTO_HASH_C),
                       new FieldOverride(ProfilePhotoRecord.PHOTO_MIME_TYPE,
                                         ProfileRecord.PHOTO_MIME_TYPE_C),
                       new FieldOverride(ProfilePhotoRecord.PHOTO_CONSTRAINT,
                                         ProfileRecord.PHOTO_CONSTRAINT_C));
    }

    /**
     * Stores the supplied profile record in the database, overwriting an previously stored profile
     * data.
     *
     * @return true if the profile was created, false if it was updated.
     */
    public boolean storeProfile (ProfileRecord record)
        throws PersistenceException
    {
        return store(record);
    }
}
