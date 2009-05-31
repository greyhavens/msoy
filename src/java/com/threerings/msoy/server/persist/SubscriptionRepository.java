//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.expression.ValueExp;
import com.samskivert.depot.operator.And;
import com.samskivert.depot.operator.Equals;
import com.samskivert.depot.operator.GreaterThan;
import com.samskivert.depot.operator.LessThan;
import com.samskivert.depot.operator.Sub;

import com.threerings.presents.annotation.BlockingThread;

import static com.threerings.msoy.Log.log;

@Singleton @BlockingThread
public class SubscriptionRepository extends DepotRepository
{
    @Inject public SubscriptionRepository (PersistenceContext ctx)
    {
        super(ctx);

        ctx.registerMigration(SubscriptionRecord.class, new SchemaMigration.Drop(2, "endDate"));
        ctx.registerMigration(SubscriptionRecord.class,
            new SchemaMigration.Retype(2, SubscriptionRecord.LAST_GRANT));
    }

    /**
     * Note a payment made to a new or existing subscription.
     */
    public boolean noteSubscriptionStarted (int memberId, int barGrantsLeft)
    {
        // always just create a new record and blast away anything previous
        // TODO: review that decision
        boolean grantBarsNow = (barGrantsLeft > 0);
        SubscriptionRecord rec = new SubscriptionRecord();
        rec.memberId = memberId;
        rec.subscriber = true;
        rec.grantsLeft = Math.max(0, barGrantsLeft - 1);
        rec.lastGrant = grantBarsNow ? new Timestamp(System.currentTimeMillis()) : null;
        store(rec);
        return grantBarsNow;
    }

    /**
     * Note a payment made to a new or existing subscription.
     */
    public int noteSubscriptionEnded (int memberId)
    {
        SubscriptionRecord rec =
            load(SubscriptionRecord.class, SubscriptionRecord.getKey(memberId));
        if (rec == null) {
            log.warning("That's weird: unable to find SubscriptionRecord to note ending.",
                "memberId", "memberId", new Exception());
            return 0; // but don't throw... I guess it's more or less OK since they're not marked.
            // But: we could also insert a record and just note that they've ended now.
        }
        int barGrantsLeft = rec.grantsLeft; // should be 0!
        rec.subscriber = false;
        rec.grantsLeft = 0;
        update(rec);
        return barGrantsLeft;
    }

    /**
     * Load the memberIds of any subscribers that are due for their monthly bar grants.
     */
    public List<Integer> loadSubscribersNeedingBarGrants ()
    {
        // TODO: this needs double-checking and testing, and some more checking
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        Timestamp monthAgo = new Timestamp(cal.getTimeInMillis());
        List<Key<SubscriptionRecord>> keys = findAllKeys(SubscriptionRecord.class, true,
            new Where(new And(
                new Equals(SubscriptionRecord.SUBSCRIBER, true),
                new GreaterThan(SubscriptionRecord.GRANTS_LEFT, 0),
                new LessThan(SubscriptionRecord.LAST_GRANT, monthAgo))));
        return Lists.transform(keys, new Function<Key<SubscriptionRecord>,Integer>() {
            public Integer apply (Key<SubscriptionRecord> key) {
                return (Integer) key.getValues()[0];
            }
        });
    }

    /**
     * Note that the specified subscriber has had their bars granted them.
     */
    public void noteBarsGranted (int memberId)
    {
        Map<ColumnExp,SQLExpression> updates = Maps.newHashMap();
        updates.put(SubscriptionRecord.LAST_GRANT,
            new ValueExp(new Timestamp(System.currentTimeMillis())));
        updates.put(SubscriptionRecord.GRANTS_LEFT, new Sub(SubscriptionRecord.GRANTS_LEFT, 1));

        int count = updatePartial(SubscriptionRecord.getKey(memberId), updates);
        if (count == 0) {
            throw new RuntimeException("SubscriptionRecord not found for " + memberId);
        }
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(SubscriptionRecord.class);
    }
}
