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
     * Loads the spam records for each member id in the given set.
     */
    public SpamRecord loadSpamRecord (Integer memberId)
    {
        return load(SpamRecord.class, memberId);
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
        spamRec.retentionEmailCount++;
        spamRec.retentionEmailCountSinceLastLogin++;
        spamRec.lastRetentionEmailSent = new Date(System.currentTimeMillis());
        spamRec.lastRetentionEmailResult = -1;
        if (newRecord) {
            insert(spamRec);

        } else {
            // do the update, just retention fields
            updatePartial(SpamRecord.getKey(spamRec.memberId),
                SpamRecord.LAST_RETENTION_EMAIL_SENT, spamRec.lastRetentionEmailSent,
                SpamRecord.RETENTION_EMAIL_COUNT, spamRec.retentionEmailCount,
                SpamRecord.LAST_RETENTION_EMAIL_RESULT, spamRec.lastRetentionEmailResult,
                SpamRecord.RETENTION_EMAIL_COUNT_SINCE_LAST_LOGIN,
                    spamRec.retentionEmailCountSinceLastLogin);
        }
    }

    public void noteRetentionEmailResult (int memberId, int cause)
    {
        updatePartial(SpamRecord.getKey(memberId), SpamRecord.LAST_RETENTION_EMAIL_RESULT, cause);
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(SpamRecord.class);
        classes.add(OptOutRecord.class);
    }
}
