//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.depot.CacheKey;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistenceContext.CacheListener;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.operator.Arithmetic;
import com.samskivert.jdbc.depot.operator.Conditionals;
import com.samskivert.jdbc.depot.operator.Logic;

import com.samskivert.util.IntIntMap;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.server.MsoyEventLogger;

import static com.threerings.msoy.Log.log;

/**
 * Manages persistent information stored on a per-member basis.
 */
@Singleton @BlockingThread
public class FlowRepository extends DepotRepository
{
    /**
     * Creates a flow repository for.
     */
    @Inject public FlowRepository (PersistenceContext ctx)
    {
        super(ctx);

        // add a cache invalidator that listens to MemberRecord updates
        _ctx.addCacheListener(MemberRecord.class, new CacheListener<MemberRecord>() {
            public void entryInvalidated (CacheKey key, MemberRecord member) {
                _ctx.cacheInvalidate(MemberFlowRecord.getKey(member.memberId));
            }
            public void entryCached (CacheKey key, MemberRecord newEntry, MemberRecord oldEntry) {
                // TODO: To be fancy, construct & cache our own MemberFlowRecord here
            }
            public String toString () {
                return "MemberRecord -> MemberFlowRecord";
            }
        });
    }

    /**
     * Fetch the computed {@link MemberFlowRecord} for the given member. This effectively
     * executes 'select flow from MemberRecord where memberId=?'.
     *
     * TODO: This should probably not be cached at all.
     */
    public MemberFlowRecord loadMemberFlow (int memberId)
        throws PersistenceException
    {
        return load(MemberFlowRecord.class, memberId, new FromOverride(MemberRecord.class));
    }

    /**
     * Logs an action for a member with optional action-specific data, which may be null.
     *
     * @return null if no flow was granted as a result of this action, the member's new
     * MemberFlowRecord if flow was granted by the action.
     */
    public MemberFlowRecord logUserAction (UserActionDetails info)
        throws PersistenceException
    {
        // if they get flow for performing this action, grant it to them
        if (info.action.getFlow() > 0) {
            return grantFlow(info, info.action.getFlow());
        } else {
            recordUserAction(info);
            return null;
        }
    }

    /**
     * Assess this member's humanity based on actions taken between the last assessment and now.
     */
    public int assessHumanity (int memberId, int currentHumanity, int secsSinceLast)
        throws PersistenceException
    {
        // load up all of their actions since our last humanity assessment
        Where condition = new Where(MemberActionLogRecord.MEMBER_ID_C, memberId);
        List<MemberActionLogRecord> records = findAll(MemberActionLogRecord.class, condition);

        // if they've done nothing of note, do not adjust their humanity
        if (records.size() == 0) {
            return currentHumanity;
        }

        // summarize their action counts and compute any humanity adjustment
        HumanityHelper helper = new HumanityHelper();
        IntIntMap actionCounts = new IntIntMap();
        for (MemberActionLogRecord record : records) {
            // note that they performed this action once
            actionCounts.increment(record.actionId, 1);
            // tell our humanity helper about the record
            helper.noteRecord(record);
        }

        Map<String, SQLExpression> fieldMap = Maps.newHashMap();

        // update their action summary counts
        for (IntIntMap.IntIntEntry entry : actionCounts.entrySet()) {
            fieldMap.put(
                MemberActionSummaryRecord.COUNT,
                new Arithmetic.Add(MemberActionSummaryRecord.COUNT_C, entry.getIntValue()));
            int rows = updateLiteral(
                MemberActionSummaryRecord.class,
                MemberActionSummaryRecord.MEMBER_ID, memberId,
                MemberActionSummaryRecord.ACTION_ID, entry.getIntKey(),
                fieldMap);
            // if no row was modified, this must be a first time action by this member
            if (rows == 0) {
                // so create a new row
                insert(new MemberActionSummaryRecord(
                    memberId, entry.getIntKey(), entry.getIntValue()));
            }
        }

        // clear their log tables -- no cache invalidation needed because these records do not
        // define a primary key at all
        deleteAll(MemberActionLogRecord.class, condition, null);

        // finally compute a new humanity assessment for this member (TODO: load up action summary
        // counts, pass that data in as well)
        return helper.computeNewHumanity(memberId, currentHumanity, secsSinceLast);
    }

    /**
     * <em>Do not use this method!</em> It exists only because we must work with the coin system
     * which tracks members by username rather than id.
     */
    public int grantFlow (UserActionDetails action, String accountName, int amount)
        throws PersistenceException
    {
        MemberRecord record =
            load(MemberRecord.class, new Where(MemberRecord.ACCOUNT_NAME_C, accountName));
        if (record == null) {
            throw new PersistenceException(
                "Unknown member [accountName=" + accountName + ", action=" + action + "]");
        }

        final UserActionDetails newInfo = new UserActionDetails(
            record.memberId, action.action, action.otherMemberId, action.itemType, action.itemId);
        return grantFlow(newInfo, amount).flow;
    }

    /**
     * Expire a member's flow given that dMin minute passed since last we did so.  The flow field
     * of the supplied MemberRecord is modified by this method: the expired flow is subtracted from
     * it.
     */
    public void expireFlow (MemberRecord record, float dMin)
        throws PersistenceException
    {
        float dailyFactor = (1 - RuntimeConfig.server.dailyFlowEvaporation);
        int newFlow = (int) (record.flow * Math.pow(dailyFactor, dMin/DAY_MINS));
        if (newFlow != record.flow) {
            record.flow = newFlow;
            update(record);
        }
    }

    /**
     * Deducts the specified amount of flow from the supplied member's record. The supplied user
     * action will also be logged with a {@link MemberActionLogRecord}.
     *
     * @return the member's new flow value following the update.
     */
    public MemberFlowRecord spendFlow (UserActionDetails info, int amount)
        throws PersistenceException
    {
        return updateFlow(info, amount, false, false);
    }

    /**
     * Adds the specified amount of flow to the supplied member's record. The supplied user action
     * will also be logged with a {@link MemberActionLogRecord}.
     *
     * @return the member's new flow value following the update.
     */
    public MemberFlowRecord grantFlow (UserActionDetails info, int amount)
        throws PersistenceException
    {
        return updateFlow(info, amount, true, true);
    }

    /**
     * Returns the specified amount of flow to the supplied member's record. This flow will not be
     * credited as "accumulated" as the assumption is this flow is being granted as a result of a
     * refunded purchase.
     *
     * @return the member's new flow value following the update.
     */
    public MemberFlowRecord refundFlow (UserActionDetails info, int amount)
        throws PersistenceException
    {
        return updateFlow(info, amount, true, false);
    }

    /**
     * Helper function for {@link #spendFlow} and {@link #grantFlow}.
     *
     * @return the user's new flow value following the update.
     */
    protected MemberFlowRecord updateFlow (
        UserActionDetails info, int amount, boolean grant, boolean accumulate)
        throws PersistenceException
    {
        String type = (grant ? "grant" : " spend");
        if (amount <= 0) {
            throw new PersistenceException(
                "Illegal flow " + type +
                " [memberId=" + info.memberId + ", amount=" + amount + "]");
        }

        Map<String, SQLExpression> fieldMap = Maps.newHashMap();
        fieldMap.put(MemberRecord.FLOW, grant ?
            new Arithmetic.Add(MemberRecord.FLOW_C, amount) :
            new Arithmetic.Sub(MemberRecord.FLOW_C, amount));
        if (grant && accumulate) {
            // accumulate positive flow updates in its own field
            fieldMap.put(MemberRecord.ACC_FLOW,
                         new Arithmetic.Add(MemberRecord.ACC_FLOW_C, amount));
        }

        Key<MemberRecord> key = MemberRecord.getKey(info.memberId);
        int mods;
        if (grant) {
            mods = updateLiteral(MemberRecord.class, key, key, fieldMap);
            if (mods == 0) {
                throw new PersistenceException(
                    "Grant modified zero rows!? " +
                    "[mid=" + info.memberId + ", amount=" + amount + "]");
            }

        } else {
            mods = updateLiteral(
                MemberRecord.class,
                new Where(new Logic.And(
                              new Conditionals.Equals(MemberRecord.MEMBER_ID_C, info.memberId),
                              new Conditionals.GreaterThanEquals(MemberRecord.FLOW_C, amount))),
                key, fieldMap);
            if (mods == 0) {
                throw new PersistenceException(
                    "Spend modified zero rows (probably NSF) " +
                    "[mid=" + info.memberId + ", amount=" + amount + "]");
            }
        }

        // sanity check
        if (mods > 1) {
            log.warning("Flow " + type + " modified multiple rows [mid=" + info.memberId +
                        ", amount=" + amount + ", mods=" + mods + "].");
        }

        Date date = new Date(System.currentTimeMillis());
        boolean again = false;
        do {
            fieldMap.clear();
            fieldMap.put(DailyFlowRecord.AMOUNT,
                         new Arithmetic.Add(DailyFlowRecord.AMOUNT_C, amount));
            Key<DailyFlowRecord> dailyFlowKey = DailyFlowRecord.getKey(type, date);
            mods = updateLiteral(DailyFlowRecord.class, dailyFlowKey, dailyFlowKey, fieldMap);
            if (mods == 0) {
                // if this is the second time we tried that update, flip out.
                if (again) {
                    throw new PersistenceException(
                        "Flow summary update modified zero rows after insertion " +
                        "[memberId=" + info.memberId + ", amount=" + amount + "]");
                }
                DailyFlowRecord summary = new DailyFlowRecord();
                summary.date = date;
                summary.type = type;
                summary.amount = amount;
                try {
                    insert(summary);
                } catch (PersistenceException p) {
                    if (p.getCause() instanceof SQLException &&
                        p.getCause().getMessage() != null &&
                        p.getCause().getMessage().indexOf("Duplicate entry") != -1) {
                        // another server got precisely the same insertion into place before us
                        // so loop back and do the update
                        again = true;
                    } else {
                        throw p;
                    }
                }
            }
        } while (again);

        // record the associated user action
        recordUserAction(info);

        // TODO: can we magically get the updated value from the database? stored procedure?
        MemberFlowRecord updatedFlow = loadMemberFlow(info.memberId);

        // record this flow transaction to our uber log
        _eventLog.flowTransaction(info, grant ? amount : -amount, updatedFlow.flow);

        return updatedFlow;
    }

    /**
     * Records a user action in the database.
     */
    protected void recordUserAction (UserActionDetails info)
        throws PersistenceException
    {
        MemberActionLogRecord record = new MemberActionLogRecord();
        record.memberId = info.memberId;
        record.actionId = info.action.getNumber();
        record.actionTime = new Timestamp(System.currentTimeMillis());
        record.data = info.misc;
        insert(record);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(DailyFlowRecord.class);
        classes.add(MemberFlowRecord.class);
        classes.add(MemberActionLogRecord.class);
        classes.add(MemberActionSummaryRecord.class);
    }

    /** Minutes in a day. */
    protected static final float DAY_MINS = 24 * 60;

    /** Reference to the event logger. */
    @Inject protected MsoyEventLogger _eventLog;
}
