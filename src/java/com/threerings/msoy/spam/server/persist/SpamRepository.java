//
// $Id$

package com.threerings.msoy.spam.server.persist;

import java.util.Set;

import java.sql.Date;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;

import com.threerings.presents.annotation.BlockingThread;

/**
 * Coordinates persistence for data related to external emailing of members.
 */
@Singleton @BlockingThread
public class SpamRepository extends DepotRepository
{
    @Inject public SpamRepository (PersistenceContext ctx)
    {
        super(ctx);

        ctx.registerMigration(SpamRecord.class, new SchemaMigration.Rename(3,
            "lastRetentionEmailResult", SpamRecord.RETENTION_STATUS));
        ctx.registerMigration(SpamRecord.class, new SchemaMigration.Rename(3,
            "lastRetentionEmailSent", SpamRecord.RETENTION_SENT));
        ctx.registerMigration(SpamRecord.class, new SchemaMigration.Rename(3,
            "retentionEmailCount", SpamRecord.RETENTION_COUNT));
        ctx.registerMigration(SpamRecord.class, new SchemaMigration.Rename(3,
            "retentionEmailCountSinceLastLogin", SpamRecord.RETENTION_COUNT_SINCE_LOGIN));
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
        return load(OptOutRecord.getKey(email.toLowerCase())) != null;
    }

    /**
     * Loads the spam records for each member id in the given set.
     */
    public SpamRecord loadSpamRecord (Integer memberId)
    {
        return load(SpamRecord.getKey(memberId));
    }

    /**
     * Updates all statistics associated with sending a retention email to the given user id. The
     * previously loaded record should be passed in to avoid re-reading it and do consistency
     * checking.
     * TODO: consistency checking
     */
    public void noteRetentionEmailSending (int memberId, SpamRecord spamRec)
    {
        boolean newRecord = spamRec == null;
        if (spamRec == null) {
            spamRec = new SpamRecord();
            spamRec.memberId = memberId;
        }
        spamRec.retentionCount++;
        spamRec.retentionCountSinceLogin++;
        spamRec.retentionSent = new Date(System.currentTimeMillis());
        spamRec.retentionStatus = -1;
        if (newRecord) {
            insert(spamRec);

        } else {
            // do the update, just retention fields
            updatePartial(SpamRecord.getKey(spamRec.memberId),
                SpamRecord.RETENTION_SENT, spamRec.retentionSent,
                SpamRecord.RETENTION_COUNT, spamRec.retentionCount,
                SpamRecord.RETENTION_STATUS, spamRec.retentionStatus,
                SpamRecord.RETENTION_COUNT_SINCE_LOGIN, spamRec.retentionCountSinceLogin);
        }
    }

    public void noteRetentionEmailResult (int memberId, int cause)
    {
        updatePartial(SpamRecord.getKey(memberId), SpamRecord.RETENTION_STATUS, cause);
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(SpamRecord.class);
        classes.add(OptOutRecord.class);
    }
}
