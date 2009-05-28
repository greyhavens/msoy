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
    public void noteSubscription (int memberId, long endTime)
    {
        // always just create a new record and blast away anything previous
        SubscriptionRecord rec = new SubscriptionRecord();
        rec.memberId = memberId;
        rec.endDate = new Timestamp(endTime);
        rec.lastGrant = new Timestamp(System.currentTimeMillis());
        store(rec);
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(SubscriptionRecord.class);
    }
}
