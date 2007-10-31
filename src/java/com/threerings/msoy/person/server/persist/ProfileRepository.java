//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.*;

import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRecord;

/**
 * Manages the persistent store of profile profile data.
 */
public class ProfileRepository extends DepotRepository
{
    public ProfileRepository (PersistenceContext ctx)
    {
        super(ctx);

        // TEMP
        _ctx.registerMigration(ProfileRecord.class, new EntityMigration.Drop(7, "isMale"));
        // END TEMP
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
    public List<ProfileRecord> loadProfiles (Set<Integer> memberIds)
        throws PersistenceException
    {
        if (memberIds.size() == 0) {
            return Collections.emptyList();
        } else {
            Where where = new Where(new In(ProfileRecord.MEMBER_ID_C, memberIds));
            return findAll(ProfileRecord.class, where);
        }
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

    /**
     * Finds the member name records for the members who's first and last names match the search
     * parameter.  This currently assumes the first word in <code>search</code> is the first name,
     * and the last word is the last name.
     */
    public List<MemberNameRecord> findMemberNames (String search, int maxRecords)
        throws PersistenceException
    {
        if (search == null) {
            return Collections.emptyList();
        }

        return findAll(MemberNameRecord.class,
                       new FromOverride(MemberRecord.class),
                       new Join(MemberRecord.MEMBER_ID_C, ProfileRecord.MEMBER_ID_C),
                       new Where(new FullTextMatch(
                           ProfileRecord.class, ProfileRecord.FTS_REAL_NAME, search)),
                       new Limit(0, maxRecords));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ProfileRecord.class);
    }
}
