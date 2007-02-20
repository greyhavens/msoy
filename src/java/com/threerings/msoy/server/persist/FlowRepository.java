//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;
import java.util.Collection;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.depot.CacheInvalidator;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Logic.*;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntMap;

import static com.threerings.msoy.Log.log;

/**
 * Manages persistent information stored on a per-member basis.
 */
public class FlowRepository extends DepotRepository
{
    // TODO: most likely make these enums
    public static final int ACTION_CREATED_PROFILE              = 1;
    public static final int ACTION_UPDATED_PROFILE              = 2;
    
    public static final int ACTION_SENT_FRIEND_INVITE           = 10;
    public static final int ACTION_ACCEPTED_FRIEND_INVITE       = 11;
    
    public static final int ACTION_PLAYED_GAME                  = 20;

    public static final int ACTION_CREATED_ITEM                 = 30;
    public static final int ACTION_BOUGHT_ITEM                  = 31;
    public static final int ACTION_LISTED_ITEM                  = 32;

    /**
     * Creates a flow repository for.
     */
    public FlowRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Logs an action for a member with optional action-specific data, which may be null.
     */
    public void logMemberAction (int memberId, int actionId, String data)
        throws PersistenceException
    {
        MemberActionLogRecord record = new MemberActionLogRecord();
        record.memberId = memberId;
        record.actionId = actionId;
        record.actionTime = new Timestamp(System.currentTimeMillis());
        record.data = data;
        insert(record);
    }
    
    /**
     * Updates the action summary count for the given member and action.
     */
    public void updateMemberActionSummary (int memberId, int actionId, int count)
        throws PersistenceException
    {
        updateLiteral(
            MemberActionSummaryRecord.class,
            MemberActionSummaryRecord.MEMBER_ID, memberId,
            MemberActionSummaryRecord.ACTION_ID, actionId,
            new String[] { MemberActionSummaryRecord.COUNT,
                           MemberActionSummaryRecord.COUNT + "+" + count });
    }

    /**
     * Assess this member's humanity based on actions taken between the last assessment and now.
     */
    public int assessHumanity (int memberId, int currentHumanity,
                               Timestamp lastAssessment, Timestamp now)
        throws PersistenceException
    {
        // create the condition that specifices which log records we're interested in
        Where condition = new Where(new And(
            new Equals(MemberActionLogRecord.MEMBER_ID, memberId),
            new GreaterThan(MemberActionLogRecord.ACTION_TIME_C, lastAssessment),
            new LessThanEquals(MemberActionLogRecord.ACTION_TIME_C, now)));

        // load all actions logged since our last assessment
        Collection<MemberActionLogRecord> records =
            findAll(MemberActionLogRecord.class, condition);

        if (records.size() > 0) {
            // count how many of each action there were
            IntIntMap actionTotals = new IntIntMap();
            for (MemberActionLogRecord record : records) {
                actionTotals.increment(record.actionId, 1);
            }
            // then iterate over the actions
            int[] keys = actionTotals.getKeys();
            int[] counts = actionTotals.getValues();
            for (int i = 0; i < keys.length; i ++) {
                // and update each summary table
                updateLiteral(
                    MemberActionSummaryRecord.class,
                    MemberActionSummaryRecord.MEMBER_ID, memberId,
                    MemberActionSummaryRecord.ACTION_ID, keys[i],
                    new String[] {
                        MemberActionSummaryRecord.COUNT,
                        MemberActionSummaryRecord.COUNT + " + " + counts[i],
                    });
            }
            // if all went well, clear the log tables -- no cache invalidation needed because
            // these records do not define a primary key at all
            deleteAll(MemberActionLogRecord.class, condition, null);

            // bump humanity by about 0.1 if we did anything at all in the past 24+ hours
            currentHumanity = Math.min(currentHumanity + 30, 255);
        }
        return currentHumanity;
    }


    
    /**
     * Deducts the specified amount of flow from the specified member's account.
     */
    public void spendFlow (int memberId, int amount)
        throws PersistenceException
    {
        updateFlow(MemberRecord.MEMBER_ID, memberId, amount, "spend");
    }

    /**
     * Adds the specified amount of flow to the specified member's account.
     */
    public void grantFlow (int memberId, int amount)
        throws PersistenceException
    {
        updateFlow(MemberRecord.MEMBER_ID, memberId, amount, "grant");
    }

    /**
     * <em>Do not use this method!</em> It exists only because we must work with the coin system
     * which tracks members by username rather than id.
     */
    public void grantFlow (String accountName, int amount)
        throws PersistenceException
    {
        updateFlow(MemberRecord.ACCOUNT_NAME, accountName, amount, "grant");
    }

    /** Helper function for {@link #spendFlow} and {@link #grantFlow}. */
    protected void updateFlow (String index, Comparable key, int amount, String type)
        throws PersistenceException
    {
        if (amount <= 0) {
            throw new PersistenceException(
                "Illegal flow " + type + " [index=" + index + ", amount=" + amount + "]");
        }

        String op = type.equals("grant") ? "+" : "-";
        // TODO: Cache Invalidation
        int mods = updateLiteral(MemberRecord.class, new Where(index, key), null,
                                 MemberRecord.FLOW, MemberRecord.FLOW + op + amount);
        if (mods == 0) {
            throw new PersistenceException(
                "Flow " + type + " modified zero rows " +
                "[where=" + index + "=" + key + ", amount=" + amount + "]");
        } else if (mods > 1) {
            log.warning("Flow " + type + " modified multiple rows " +
                        "[where=" + index + "=" + key + ", amount=" + amount +
                        ", mods=" + mods + "].");
        }
    }

}
