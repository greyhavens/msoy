//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;

import com.threerings.presents.annotation.BlockingThread;

import static com.threerings.msoy.Log.log;

@Singleton @BlockingThread
public class SubscriptionRepository extends DepotRepository
{
    @Inject public SubscriptionRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Note a payment made to a new or existing subscription.
     */
    public void noteSubscriptionStarted (int memberId, long endTime)
    {
        // always just create a new record and blast away anything previous
        SubscriptionRecord rec = new SubscriptionRecord();
        rec.memberId = memberId;
        rec.endDate = new Timestamp(endTime);
        rec.lastGrant = new Timestamp(System.currentTimeMillis());
        store(rec);
    }

    /**
     * Note a payment made to a new or existing subscription.
     */
    public void noteSubscriptionEnded (int memberId)
    {
        SubscriptionRecord rec =
            load(SubscriptionRecord.class, SubscriptionRecord.getKey(memberId));
        if (rec == null) {
            log.warning("That's weird: unable to find SubscriptionRecord to note ending.",
                "memberId", "memberId", new Exception());
            return; // but don't throw... I guess it's more or less OK since they're not marked.
            // But: we could also insert a record and just note that they've ended now.
        }
        // only update the time if we're going to lower it
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (now.before(rec.endDate)) {
            rec.endDate = now;
            update(rec);
        }
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(SubscriptionRecord.class);
    }
}
