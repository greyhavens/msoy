//
// $Id$

package com.threerings.msoy.badge.server.persist;

import java.util.Set;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Where;

import com.threerings.presents.annotation.BlockingThread;

// TEMP
import com.samskivert.jdbc.depot.CacheInvalidator;
import com.samskivert.jdbc.depot.expression.LiteralExp;
import com.samskivert.util.CountHashMap;

import com.threerings.stats.data.IntStatIncrementer;
import com.threerings.stats.data.Stat;
import com.threerings.stats.server.persist.StatRecord;
import com.threerings.stats.server.persist.StatRepository;

import com.threerings.msoy.data.StatType;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.server.persist.FriendRecord;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import static com.threerings.msoy.Log.log;
// END TEMP

@Singleton @BlockingThread
public class BadgeRepository extends DepotRepository
{
    @Inject public BadgeRepository (PersistenceContext perCtx)
    {
        super(perCtx);
    }

    // TEMP
    public void migrateStats ()
        throws PersistenceException
    {
        log.info("Populating initial stat records. Please hold...");

        // first, we want to drop all stats and badges
        CacheInvalidator NOOP = new CacheInvalidator() {
            public void invalidate (PersistenceContext ctx) { /* NOOP! */ }
        };
        deleteAll(StatRecord.class, new Where(new LiteralExp("1 = 1")), NOOP);
        deleteAll(EarnedBadgeRecord.class, new Where(new LiteralExp("1 = 1")), NOOP);
        deleteAll(InProgressBadgeRecord.class, new Where(new LiteralExp("1 = 1")), NOOP);

        // count up how many friends everyone has
        final CountHashMap<Integer> friends = new CountHashMap<Integer>();
        for (FriendRecord frec : findAll(FriendRecord.class)) {
            friends.incrementCount(frec.inviterId, 1);
            friends.incrementCount(frec.inviteeId, 1);
        }

        // count up how many accepted invitations everyone has
        final CountHashMap<Integer> invites = new CountHashMap<Integer>();
        for (InvitationRecord irec : findAll(InvitationRecord.class)) {
            if (irec.inviterId != 0 && irec.inviteeId != 0) {
                invites.incrementCount(irec.inviterId, 1);
            }
        }

        // count up how many trophies everyone has earned
        final CountHashMap<Integer> trophies = new CountHashMap<Integer>();
        for (TrophyRecord trec : findAll(TrophyRecord.class)) {
            trophies.incrementCount(trec.memberId, 1);
        }

        // now we want to do a bunch of per-user stuff
        final int[] created = new int[3];
        _memberRepo.runMemberMigration(new MemberRepository.MemberMigration() {
            public void apply (MemberRecord record) throws PersistenceException {
                // write out stats for the data we collected previously
                Stat.Type[] stats = new Stat.Type[] {
                    StatType.FRIENDS_MADE, StatType.INVITES_ACCEPTED, StatType.TROPHIES_EARNED
                };
                int[] values = new int[] {
                    friends.getCount(record.memberId), invites.getCount(record.memberId),
                    trophies.getCount(record.memberId)
                };
                for (int ii = 0; ii < stats.length; ii++) {
                    if (values[ii] > 0) {
                        _statRepo.updateStat(
                            record.memberId, new IntStatIncrementer(stats[ii], values[ii]));
                    }
                    created[ii]++;
                }
            }
        });

        log.info("Populated initial stat information", "friends", created[0], "invites", created[1],
                 "trophies", created[2]);
    }
    // END TEMP

    /**
     * Stores the supplied badge record in the database.
     */
    public void storeBadge (EarnedBadgeRecord badge)
        throws PersistenceException
    {
        store(badge);
    }

    /**
     * Stores the supplied in-progress badge record in the database.
     */
    public void storeInProgressBadge (InProgressBadgeRecord badge)
        throws PersistenceException
    {
        store(badge);
    }

    /**
     * Loads all of the specific member's earned badges.
     */
    public List<EarnedBadgeRecord> loadEarnedBadges (int memberId)
        throws PersistenceException
    {
        return findAll(EarnedBadgeRecord.class, new Where(EarnedBadgeRecord.MEMBER_ID_C, memberId));
    }

    /**
     * Loads all of the specified member's in-progress badges.
     */
    public List<InProgressBadgeRecord> loadInProgressBadges (int memberId)
        throws PersistenceException
    {
        return findAll(InProgressBadgeRecord.class, new Where(InProgressBadgeRecord.MEMBER_ID_C,
            memberId));
    }

    /**
     * Loads the EarnedBadgeRecord, if it exists, for the specified member and badge type.
     */
    public EarnedBadgeRecord loadEarnedBadge (int memberId, int badgeCode)
        throws PersistenceException
    {
        return load(EarnedBadgeRecord.class, EarnedBadgeRecord.getKey(memberId, badgeCode));
    }

    /**
     * Loads the InProgressBadgeRecord, if it exists, for the specified member and badge type.
     */
    public InProgressBadgeRecord loadInProgressBadge (int memberId, int badgeCode)
        throws PersistenceException
    {
        return load(InProgressBadgeRecord.class, InProgressBadgeRecord.getKey(memberId, badgeCode));
    }

    /**
     * Deletes the InProgressBadgeRecord, if it exists, for the specified member and badge type.
     *
     * @return true if the record was deleted; false if it did not exist
     */
    public boolean deleteInProgressBadge (int memberId, int badgeCode)
        throws PersistenceException
    {
        return (delete(InProgressBadgeRecord.class, InProgressBadgeRecord.getKey(memberId,
            badgeCode)) > 0);
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(EarnedBadgeRecord.class);
        classes.add(InProgressBadgeRecord.class);
    }

    // TEMP
    @Inject protected MemberRepository _memberRepo;
    @Inject protected StatRepository _statRepo;
    // END TEMP
}
