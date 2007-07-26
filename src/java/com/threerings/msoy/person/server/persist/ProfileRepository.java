//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.IntListUtil;

import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.Equals;
import com.samskivert.jdbc.depot.operator.Conditionals.In;
import com.samskivert.jdbc.depot.operator.Conditionals.Like;
import com.samskivert.jdbc.depot.operator.Logic.And;

import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages the persistent store of profile profile data.
 */
public class ProfileRepository extends DepotRepository
{
    public ProfileRepository (PersistenceContext ctx)
    {
        super(ctx);
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
    public List<ProfileRecord> loadProfiles (int[] memberIds)
        throws PersistenceException
    {
        // In() requires at least one value
        if (memberIds == null || memberIds.length == 0) {
            return new ArrayList<ProfileRecord>();
        }
        return findAll(ProfileRecord.class,
                       new Where(new In(ProfileRecord.MEMBER_ID_C, IntListUtil.box(memberIds))));
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
            return new ArrayList<MemberNameRecord>();
        }

        return findAll(MemberNameRecord.class,
                       new FromOverride(MemberRecord.class),
                       new Join(MemberRecord.MEMBER_ID_C, ProfileRecord.MEMBER_ID_C),
                       new Where(new Like(ProfileRecord.REAL_NAME_C, "%" + search + "%")),
                       new Limit(0, maxRecords));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ProfileRecord.class);
    }
}
