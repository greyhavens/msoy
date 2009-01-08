//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.operator.Conditionals.*;
import com.samskivert.depot.operator.Logic.And;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.RecordFunctions;

import com.threerings.msoy.person.gwt.Interest;
import com.threerings.msoy.profile.gwt.Profile;

/**
 * Manages the persistent store of profile profile data.
 */
@Singleton @BlockingThread
public class ProfileRepository extends DepotRepository
{
    @Inject public ProfileRepository (PersistenceContext ctx)
    {
        super(ctx);

        // 142 MemberRecords from the early days do not have corresponding ProfileRecords
        registerMigration(new DataMigration("2008_12_04_create_missing_ProfileRecords") {
            @Override public void invoke ()
                throws DatabaseException
            {
                // select all the MemberRecords with missing ProfileRecords
                // only search for memberIds < 400 for efficiency, only these are affected
                List<MemberRecord> members = findAll(MemberRecord.class,
                    new Join(MemberRecord.MEMBER_ID,
                        ProfileRecord.MEMBER_ID).setType(Join.Type.LEFT_OUTER),
                    new Where(new And(
                        new LessThanEquals(MemberRecord.MEMBER_ID, 400),
                        new IsNull(ProfileRecord.MEMBER_ID))
                    ));

                // create blank ProfileRecords with all defaults
                for (MemberRecord member : members) {
                    ProfileRecord profile = new ProfileRecord(member.memberId, new Profile());
                    store(profile);
                }
            }
        });
    }

    /**
     * Loads the profile record for the specified member. Returns null if no record has been
     * created for that member.
     */
    public ProfileRecord loadProfile (int memberId)
    {
        return load(ProfileRecord.class, memberId);
    }

    /**
     * Loads the profiles for all of the specified members.
     */
    public List<ProfileRecord> loadProfiles (Set<Integer> memberIds)
    {
        return loadAll(ProfileRecord.class, memberIds);
    }

    /**
     * Stores the supplied profile record in the database, overwriting an previously stored profile
     * data.
     *
     * @return true if the profile was created, false if it was updated.
     */
    public boolean storeProfile (ProfileRecord record)
    {
        return store(record);
    }

    /**
     * Loads the interests for the specified member.
     */
    public List<InterestRecord> loadInterests (int memberId)
    {
        return findAll(InterestRecord.class, new Where(InterestRecord.MEMBER_ID, memberId));
    }

    /**
     * Stores the supplied list of interests in the repository, overwriting
     * any previously stored interests for this member. Any interests with the
     * empty string as {@link Interest#interests} will be deleted.
     */
    public void storeInterests (int memberId, List<Interest> interests)
    {
        InterestRecord record = new InterestRecord();
        record.memberId = memberId;

        for (Interest interest : interests) {
            record.type = interest.type;
            if (interest.interests.equals("")) {
                delete(record);
            } else {
                record.interests = interest.interests;
                store(record);
            }
        }
    }

    /**
     * Finds the ids of members who's real names match the search parameter.
     */
    public List<Integer> findMembersByRealName (String search, int limit)
    {
        Where where = new Where(
            new FullTextMatch(ProfileRecord.class, ProfileRecord.FTS_REAL_NAME, search));
        return Lists.transform(findAllKeys(ProfileRecord.class, false, where, new Limit(0, limit)),
                               RecordFunctions.<ProfileRecord>getIntKey());
    }

    /**
     * Finds the ids of members who list the supplied term among their interests.
     */
    public List<Integer> findMembersByInterest (String search, int limit)
    {
        Where where = new Where(
            new FullTextMatch(InterestRecord.class, InterestRecord.FTS_INTERESTS, search));
        Set<Integer> ids = Sets.newHashSet();

        for (InterestRecord irec : findAll(InterestRecord.class, where, new Limit(0, limit))) {
            ids.add(irec.memberId);
        }

        return Lists.newArrayList(ids);
    }

    public void updateHeadline (int memberId, String headline)
    {
        updatePartial(ProfileRecord.class, memberId, ProfileRecord.HEADLINE, headline);
    }

    /**
     * Updates the award shown on the profile.  Only one or none of badgeCode and medalId should
     * be non-zero.
     */
    public void updateProfileAward (int memberId, int badgeCode, int medalId)
    {
        updatePartial(ProfileRecord.class, memberId, ProfileRecord.PROFILE_BADGE_CODE, badgeCode,
            ProfileRecord.PROFILE_MEDAL_ID, medalId);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ProfileRecord.class);
        classes.add(InterestRecord.class);
    }
}
