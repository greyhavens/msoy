//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.samskivert.depot.operator.Or;

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
     *
     * @return Tuple<grantBarsNow, grantItem>
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
        rec.grantsLeft = barGrantsLeft;
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
        // TODO: this needs double-checking and testing, and some more checking
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        Timestamp monthAgo = new Timestamp(cal.getTimeInMillis());
        List<Key<SubscriptionRecord>> keys = findAllKeys(SubscriptionRecord.class, true,
            new Where(new And(
                SubscriptionRecord.SUBSCRIBER.eq(true),
                SubscriptionRecord.GRANTS_LEFT.greaterThan(0),
                SubscriptionRecord.LAST_GRANT.lessThan(monthAgo))));
        return Lists.transform(keys, SubscriptionRecord.KEY_TO_MEMBER_ID);
    }

    /**
     * Load the memberIds of any subscribers that should be granted the specified special item.
     */
    public List<Integer> loadSubscribersNeedingItem (byte type, int itemId)
    {
        List<Key<SubscriptionRecord>> keys = findAllKeys(SubscriptionRecord.class, true,
            new Where(new And(
                SubscriptionRecord.SUBSCRIBER.eq(true),
                new Or(
                    SubscriptionRecord.SPECIAL_ITEM_TYPE.notEq(type),
                    SubscriptionRecord.SPECIAL_ITEM_ID.notEq(itemId)))));
        return Lists.transform(keys, SubscriptionRecord.KEY_TO_MEMBER_ID);
    }

    /**
     * Note that the specified subscriber has had their bars granted them.
     */
    public void noteBarsGranted (int memberId)
    {
        Map<ColumnExp,SQLExpression> updates = Maps.newHashMap();
        updates.put(SubscriptionRecord.LAST_GRANT,
            new ValueExp(new Timestamp(System.currentTimeMillis())));
        updates.put(SubscriptionRecord.GRANTS_LEFT, SubscriptionRecord.GRANTS_LEFT.minus(1));

        int count = updatePartial(SubscriptionRecord.getKey(memberId), updates);
        if (count == 0) {
            throw new RuntimeException("SubscriptionRecord not found for " + memberId);
        }
    }

    /**
     * Note that the specified subscriber has been granted the specified item.
     */
    public void noteSpecialItemGranted (int memberId, byte type, int itemId)
    {
        Map<ColumnExp,SQLExpression> updates = Maps.newHashMap();
        updates.put(SubscriptionRecord.SPECIAL_ITEM_TYPE, new ValueExp(type));
        updates.put(SubscriptionRecord.SPECIAL_ITEM_ID, new ValueExp(itemId));

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
