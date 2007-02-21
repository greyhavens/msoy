//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.GroupBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.FunctionExp;
import com.samskivert.jdbc.depot.expression.LiteralExp;
import com.samskivert.jdbc.depot.operator.Logic.*;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.util.IntIntMap;

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
    public int assessHumanity (int memberId, int currentHumanity, Timestamp now)
        throws PersistenceException
    {
        // create the condition that specifices which log records we're interested in
        Where condition = new Where(new And(
            new Equals(MemberActionLogRecord.MEMBER_ID, memberId),
            new LessThanEquals(MemberActionLogRecord.ACTION_TIME_C, now)));

        Collection<MemberActionCountRecord> records = findAll(
            MemberActionCountRecord.class,
            new FromOverride(MemberActionLogRecord.class),
            condition,
            new FieldOverride(MemberActionCountRecord.ACTION_ID,
                              MemberActionLogRecord.ACTION_ID_C),
            new FieldOverride(MemberActionCountRecord.COUNT, new LiteralExp("COUNT(*)")),
            new GroupBy(MemberActionLogRecord.ACTION_ID_C));

        if (records.size() > 0) {
            // for each action, update the summary records with action counts
            for (MemberActionCountRecord record : records) {
                // and update each summary table
                updateLiteral(
                    MemberActionSummaryRecord.class,
                    MemberActionSummaryRecord.MEMBER_ID, memberId,
                    MemberActionSummaryRecord.ACTION_ID, record.actionId,
                    new String[] {
                        MemberActionSummaryRecord.COUNT,
                        MemberActionSummaryRecord.COUNT + " + " + record.count
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
     * Assess a game's anti-abuse factor based on flow grants.
     */
    public void assessAntiAbuse (int gameId)
        throws PersistenceException
    {
        GameAbuseRecord gameRecord = load(GameAbuseRecord.class, gameId);
        if (gameRecord == null) {
            gameRecord = new GameAbuseRecord();
            gameRecord.gameId = gameId;
            gameRecord.abuseFactor = 100;
            gameRecord.accumMinutesSinceLastAssessment = 0;
        }
        // load all actions logged since our last assessment
        Collection<GameFlowSummaryRecord> records =
            findAll(GameFlowSummaryRecord.class,
                    new Where(GameFlowGrantLogRecord.GAME_ID, gameId),
                    new FromOverride(GameFlowGrantLogRecord.class),
                    new FieldOverride(GameFlowSummaryRecord.GAME_ID,
                                      GameFlowGrantLogRecord.GAME_ID_C),
                    new FieldOverride(GameFlowSummaryRecord.AMOUNT,
                                      new FunctionExp("sum", GameFlowGrantLogRecord.AMOUNT_C)));

        if (records.size() > 0) {
            deleteAll(MemberActionLogRecord.class,
                      new Where(GameFlowGrantLogRecord.GAME_ID, gameId),
                      null);
            gameRecord.accumMinutesSinceLastAssessment = 0;
            // substitute actual algorithm at some point
            gameRecord.abuseFactor = 123;
            store(gameRecord);
        }
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

    @Computed
    @Entity
    protected static class MemberActionCountRecord extends PersistentRecord
    {
        public static final String ACTION_ID = "actionId";
        public static final String COUNT = "count";

        /** The id of the action this entry counts. */
        public int actionId;

        /** The number of times this action was performed (by the implicit member). */
        public int count;
    }

    @Computed
    @Entity
    protected static class GameFlowSummaryRecord extends PersistentRecord
    {
        public static final String GAME_ID = "gameId";
        public static final String AMOUNT = "amount";

        /** The id of the game that did the flow granting. */
        public int gameId;

        /** The amount of flow this game granted. */
        public int amount;
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
        Date date = new Date(System.currentTimeMillis());
        
        boolean again = false;
        do {
            mods = updateLiteral(
                DailyFlowSummary.class,
                new Where(DailyFlowSummary.GRANT_TYPE, type, DailyFlowSummary.GRANT_DATE, date),
                null,
                DailyFlowSummary.GRANTED, DailyFlowSummary.GRANTED + op + amount);
            if (mods == 0) {
                // if this is the second time we tried that update, flip out.
                if (again) {
                    throw new PersistenceException(
                        "Flow summary update modified zero rows after insertion " +
                        "[where=" + index + "=" + key + ", amount=" + amount + "]");
                }
                DailyFlowSummary summary = new DailyFlowSummary();
                summary.grantDate = date;
                summary.grantType = type;
                summary.granted = amount;
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
    }

}
