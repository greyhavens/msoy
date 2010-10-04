//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Exps;
import com.samskivert.depot.Key;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;

import com.samskivert.util.Calendars;

import com.threerings.msoy.item.data.all.MsoyItemType;
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
    public SubscriptionRecord noteSubscriptionBilled (int memberId, int barGrantsLeft)
    {
        SubscriptionRecord rec = load(SubscriptionRecord.getKey(memberId));
        if (rec == null) {
            rec = new SubscriptionRecord(); // that's ok, they are fresh meat
            rec.memberId = memberId;

        } else if (rec.grantsLeft > 0) {
            log.warning("Shazbot! Subscription started with existing bar grants remaining",
                "memberId", memberId, "grantsLeft", rec.grantsLeft, new Exception());
        }

        rec.subscriber = true;
        rec.grantsLeft += barGrantsLeft;
        // do not update their special item information.
        // That will be looked at by SubscriptionLogic
        store(rec);
        return rec;
    }

    /**
     * Note a payment made to a new or existing subscription.
     */
    public void noteSubscriptionEnded (int memberId)
    {
        SubscriptionRecord rec = load(SubscriptionRecord.getKey(memberId));
        if (rec == null) {
            log.warning("That's weird: unable to find SubscriptionRecord to note ending.",
                "memberId", "memberId", new Exception());
            return; // but don't throw... I guess it's more or less OK since they're not marked.
            // But: we could also insert a record and just note that they've ended now.

        } else if (rec.grantsLeft > 0) {
            log.warning("Shazbot! Subscription ended with bar grants remaining",
                "memberId", memberId, "grantsLeft", rec.grantsLeft, new Exception());
        }

        rec.subscriber = false;
        rec.grantsLeft = 0;
        update(rec);
    }

    /**
     * Load the memberIds of any subscribers that are due for their monthly bar grants.
     */
    public List<Integer> loadSubscribersNeedingBarGrants ()
    {
        List<Key<SubscriptionRecord>> keys = findAllKeys(SubscriptionRecord.class, true,
            new Where(Ops.and(
                SubscriptionRecord.SUBSCRIBER.eq(true),
                SubscriptionRecord.GRANTS_LEFT.greaterThan(0),
                Ops.or(
                    SubscriptionRecord.LAST_GRANT.lessThan(
                        Calendars.now().addMonths(-1).toTimestamp()),
                    SubscriptionRecord.LAST_GRANT.isNull()))));
        return Lists.transform(keys, Key.<SubscriptionRecord>toInt());
    }

    /**
     * Load the memberIds of any subscribers that should be granted the specified special item.
     */
    public List<Integer> loadSubscribersNeedingItem (MsoyItemType type, int itemId)
    {
        List<Key<SubscriptionRecord>> keys = findAllKeys(SubscriptionRecord.class, true,
            new Where(Ops.and(
                SubscriptionRecord.SUBSCRIBER.eq(true),
                Ops.or(
                    SubscriptionRecord.SPECIAL_ITEM_TYPE.notEq(type),
                    SubscriptionRecord.SPECIAL_ITEM_ID.notEq(itemId)))));
        return Lists.transform(keys, Key.<SubscriptionRecord>toInt());
    }

    /**
     * Note that the specified subscriber has had their bars granted them.
     */
    public void noteBarsGranted (int memberId)
    {
        Map<ColumnExp,SQLExpression> updates = Maps.newHashMap();
        updates.put(SubscriptionRecord.LAST_GRANT,
            Exps.value(new Timestamp(System.currentTimeMillis())));
        updates.put(SubscriptionRecord.GRANTS_LEFT, SubscriptionRecord.GRANTS_LEFT.minus(1));

        int count = updatePartial(SubscriptionRecord.getKey(memberId), updates);
        if (count == 0) {
            throw new RuntimeException("SubscriptionRecord not found for " + memberId);
        }
    }

    /**
     * Note that the specified subscriber has been granted the specified item.
     */
    public void noteSpecialItemGranted (int memberId, MsoyItemType type, int itemId)
    {
        Map<ColumnExp,SQLExpression> updates = Maps.newHashMap();
        updates.put(SubscriptionRecord.SPECIAL_ITEM_TYPE, Exps.value(type));
        updates.put(SubscriptionRecord.SPECIAL_ITEM_ID, Exps.value(itemId));

        int count = updatePartial(SubscriptionRecord.getKey(memberId), updates);
        if (count == 0) {
            throw new RuntimeException("SubscriptionRecord not found for " + memberId);
        }
    }

    /**
     * Note that the specified member has "barscribed" for one month.
     */
    public void noteBarscribed (int memberId)
    {
        // save a record with the new expiration time
        BarscriptionRecord rec = new BarscriptionRecord();
        rec.memberId = memberId;
        rec.expires = Calendars.now().addMonths(1).toTimestamp();
        store(rec);
    }

    /**
     * Load all the barscribers that are expired.
     */
    public List<Integer> loadExpiredBarscribers ()
    {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<Key<BarscriptionRecord>> keys = findAllKeys(BarscriptionRecord.class, true,
            new Where(BarscriptionRecord.EXPIRES.lessThan(now)));
        return Lists.transform(keys, Key.<BarscriptionRecord>toInt());
    }

    /**
     * Return true if a record was actually deleted.
     */
    public boolean noteBarscriptionEnded (int memberId)
    {
        return (0 < delete(BarscriptionRecord.getKey(memberId)));
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(SubscriptionRecord.class);
        classes.add(BarscriptionRecord.class);
    }
}
