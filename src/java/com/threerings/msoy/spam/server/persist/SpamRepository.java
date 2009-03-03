//
// $Id$

package com.threerings.msoy.spam.server.persist;

import java.util.Collection;
import java.util.Set;

import java.sql.Date;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;

import com.threerings.presents.annotation.BlockingThread;

/**
 * Coordinates persistence for data related to external emailing of members.
 */
@Singleton @BlockingThread
public class SpamRepository extends DepotRepository
{
    @Inject SpamRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Add an email address to the opt-out list.
     */
    public void addOptOutEmail (String email)
    {
        insert(new OptOutRecord(email.toLowerCase()));
    }

    /**
     * Returns true if the given email address is on the opt-out list
     */
    public boolean hasOptedOut (String email)
    {
        return load(OptOutRecord.class, email.toLowerCase()) != null;
    }

    /**
     * Filters the given set by removing ids of members that have had a retention email sent since
     * the given time stamp *or* have had at least the given count sent. Updates the remaining
     * members' last retention email time stamp with the given current time.
     */
    public void prepateToRetain (
        Set<Integer> memberIds, long sinceCutoff, int maxEmailCount, long now)
    {
        Date cutoff = new Date(sinceCutoff);
        Date nowDate = new Date(now);

        // first update existing spam records if they qualify
        Set<Integer> updates = Sets.newHashSet();
        for (SpamRecord rec : loadSpamRecords(memberIds)) {
            // too many messages or too recently sent
            if (rec.retentionEmailCount > maxEmailCount || (rec.lastRetentionEmailSent != null &&
                rec.lastRetentionEmailSent.after(cutoff))) {
                memberIds.remove(rec.memberId);
                continue;
            }

            // do the update, just our fields
            updatePartial(SpamRecord.getKey(rec.memberId),
                SpamRecord.LAST_RETENTION_EMAIL_SENT, nowDate,
                SpamRecord.RETENTION_EMAIL_COUNT, rec.retentionEmailCount + 1);
            updates.add(rec.memberId);
        }

        // now insert new records for anyone that wasn't updated
        for (Integer memberId : memberIds) {
            if (updates.contains(memberId)) {
                continue;
            }
            SpamRecord rec = new SpamRecord();
            rec.memberId = memberId;
            rec.lastRetentionEmailSent = nowDate;
            rec.retentionEmailCount++;
            insert(rec);
        }
    }

    /**
     * Loads the spam records for each member id in the given set.
     */
    public Collection<SpamRecord> loadSpamRecords (Set<Integer> memberIds)
    {
        Set<Key<SpamRecord>> keys = Sets.newHashSet(Iterables.transform(memberIds,
            new Function<Integer, Key<SpamRecord>>() {
                public Key<SpamRecord> apply (Integer memberId) {
                    return SpamRecord.getKey(memberId);
                }
        }));
        return loadAll(keys);
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(SpamRecord.class);
        classes.add(OptOutRecord.class);
    }
}
