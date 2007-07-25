//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.depot.CacheKey;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistenceContext.CacheListener;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.GroupBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.FunctionExp;
import com.samskivert.jdbc.depot.expression.LiteralExp;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.operator.Arithmetic;
import com.samskivert.jdbc.depot.operator.SQLOperator;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.operator.Logic.*;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.server.MsoyServer;

import static com.threerings.msoy.Log.log;

/**
 * Manages persistent information stored on a per-member basis.
 */
public class FlowRepository extends DepotRepository
{
    /**
     * Creates a flow repository for.
     */
    public FlowRepository (PersistenceContext ctx)
    {
        super(ctx);

        // let this long long column name be replaced by something more sensible
        _ctx.registerMigration(
            GameAbuseRecord.class,
            new EntityMigration.Rename(2, "accumMinutesSinceLastAssessment", "accumMinutes"));

        // add a cache invalidator that listens to MemberRecord updates
        _ctx.addCacheListener(MemberRecord.class, new CacheListener<MemberRecord>() {
            public void entryInvalidated (CacheKey key, MemberRecord member) {
                _ctx.cacheInvalidate(MemberFlowRecord.getKey(member.memberId));
            }
            public void entryCached (CacheKey key, MemberRecord newEntry, MemberRecord oldEntry) {
                // TODO: To be fancy, construct & cache our own MemberFlowRecord here
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
     * Return the current anti-abuse factor associated with the given gameId, in [0, 1).
     */
    public double getAntiAbuseFactor (int gameId)
        throws PersistenceException
    {
        return (double) getAbuseRecord(gameId, true).abuseFactor / 0x100;
    }

    /**
     * Logs an action for a member with optional action-specific data, which may be null.
     *
     * @return null if no flow was granted as a result of this action, the member's new 
     * MemberFlowRecord if flow was granted by the action.
     */
    public MemberFlowRecord logUserAction (int memberId, UserAction action, String details)
        throws PersistenceException
    {
        // if they get flow for performing this action, grant it to them
        if (action.getFlow() > 0) {
            return grantFlow(memberId, action.getFlow(), action, details);
        } else {
            recordUserAction(memberId, action, details);
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
        
        Map<String, SQLExpression> fieldMap = new HashMap<String, SQLExpression>();

        // update their action summary counts
        for (IntIntMap.IntIntEntry entry : actionCounts.entrySet()) {
            fieldMap.put(
                MemberActionSummaryRecord.COUNT,
                new Arithmetic.Add(MemberActionSummaryRecord.COUNT_C, entry.getIntValue()));
            updateLiteral(
                MemberActionSummaryRecord.class,
                MemberActionSummaryRecord.MEMBER_ID, memberId,
                MemberActionSummaryRecord.ACTION_ID, entry.getIntKey(),
                fieldMap);
        }

        // clear their log tables -- no cache invalidation needed because these records do not
        // define a primary key at all
        deleteAll(MemberActionLogRecord.class, condition, null);

        // finally compute a new humanity assessment for this member (TODO: load up action summary
        // counts, pass that data in as well)
        return helper.computeNewHumanity(memberId, currentHumanity, secsSinceLast);
    }

    /**
     * Assess a game's anti-abuse factor based on flow grants, if it's been more
     * than RuntimeConfig.server.antiAbuseReassessment player-minutes since last.
     */
    public void maybeAssessAntiAbuseFactor (int gameId, int playerMinutes)
        throws PersistenceException
    {
        if (RuntimeConfig.server.abuseFactorReassessment == 0) {
            // game abuse factor has been disabled, just return
            return;
        }
        GameAbuseRecord gameRecord = getAbuseRecord(gameId, false);
        gameRecord.accumMinutes += playerMinutes;

        if (gameRecord.accumMinutes >= RuntimeConfig.server.abuseFactorReassessment) {
            // load all actions logged since our last assessment
//            List<GameFlowSummaryRecord> records =
//                findAll(GameFlowSummaryRecord.class,
//                    new Where(GameFlowGrantLogRecord.GAME_ID_C, gameId),
//                    new FromOverride(GameFlowGrantLogRecord.class),
//                    new FieldOverride(GameFlowSummaryRecord.GAME_ID,
//                                      GameFlowGrantLogRecord.GAME_ID_C),
//                    new FieldOverride(GameFlowSummaryRecord.AMOUNT,
//                                      new FunctionExp("sum", GameFlowGrantLogRecord.AMOUNT_C)),
//                    new GroupBy(GameFlowGrantLogRecord.GAME_ID_C));
            
            // write an algorithm that actually does something with 'records' here
            gameRecord.abuseFactor = 123;
            gameRecord.accumMinutes = 0;

            // then delete the records
            deleteAll(GameFlowGrantLogRecord.class,
                      new Where(GameFlowGrantLogRecord.GAME_ID_C, gameId), null);
        }
        store(gameRecord);
    }

    /**
     * <em>Do not use this method!</em> It exists only because we must work with the coin system
     * which tracks members by username rather than id.
     */
    public int grantFlow (String accountName, int amount, UserAction action, String details)
        throws PersistenceException
    {
        MemberRecord record =
            load(MemberRecord.class, new Where(MemberRecord.ACCOUNT_NAME_C, accountName));
        if (record == null) {
            throw new PersistenceException(
                "Unknown member [accountName=" + accountName + ", action=" + action + "]");
        }
        return grantFlow(record.memberId, amount, action, details).flow;
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
    public MemberFlowRecord spendFlow (int memberId, int amount, UserAction action, String details)
        throws PersistenceException
    {
        return updateFlow(memberId, amount, action, details, false, false);
    }

    /**
     * Adds the specified amount of flow to the supplied member's record. The supplied user action
     * will also be logged with a {@link MemberActionLogRecord}.
     *
     * @return the member's new flow value following the update.
     */
    public MemberFlowRecord grantFlow (int memberId, int amount, UserAction action, String details)
        throws PersistenceException
    {
        return updateFlow(memberId, amount, action, details, true, true);
    }

    /**
     * Returns the specified amount of flow to the supplied member's record. This flow will not be
     * credited as "accumulated" as the assumption is this flow is being granted as a result of a
     * refunded purchase.
     *
     * @return the member's new flow value following the update.
     */
    public MemberFlowRecord refundFlow (int memberId, int amount, UserAction action, String details)
        throws PersistenceException
    {
        return updateFlow(memberId, amount, action, details, true, false);
    }

    /**
     * Helper function for {@link #spendFlow} and {@link #grantFlow}.
     *
     * @return the user's new flow value following the update.
     */
    protected MemberFlowRecord updateFlow (int memberId, int amount, UserAction action,
                                           String details, boolean grant, boolean accumulate)
        throws PersistenceException
    {
        String type = (grant ? "grant" : " spend");
        if (amount <= 0) {
            throw new PersistenceException(
                "Illegal flow " + type + " [memberId=" + memberId + ", amount=" + amount + "]");
        }

        String op = grant ? "+" : "-";

        Map<String, SQLExpression> fieldMap = new HashMap<String, SQLExpression>();
        fieldMap.put(MemberRecord.FLOW, grant ?
            new Arithmetic.Add(MemberRecord.FLOW_C, amount) :
            new Arithmetic.Sub(MemberRecord.FLOW_C, amount));
        if (grant && accumulate) {
            // accumulate positive flow updates in its own field
            fieldMap.put(MemberRecord.ACC_FLOW, new Arithmetic.Add(MemberRecord.ACC_FLOW_C, amount));
        }

        Key key = MemberFlowRecord.getKey(memberId);
        int mods = updateLiteral(MemberRecord.class, key, key, fieldMap);
        if (mods == 0) {
            throw new PersistenceException(
                "Flow " + type + " modified zero rows " +
                "[memberId" + memberId + ", amount=" + amount + "]");
        } else if (mods > 1) {
            log.warning("Flow " + type + " modified multiple rows " + "[memberId=" + memberId +
                        ", amount=" + amount +                 ", mods=" + mods + "].");
        }
        Date date = new Date(System.currentTimeMillis());

        boolean again = false;
        do {
            fieldMap.clear();
            fieldMap.put(
                DailyFlowRecord.AMOUNT, new Arithmetic.Add(DailyFlowRecord.AMOUNT_C, amount));
            mods = updateLiteral(
                DailyFlowRecord.class,
                new Where(DailyFlowRecord.TYPE_C, type, DailyFlowRecord.DATE_C, date),
                null, fieldMap);
            if (mods == 0) {
                // if this is the second time we tried that update, flip out.
                if (again) {
                    throw new PersistenceException(
                        "Flow summary update modified zero rows after insertion " +
                        "[memberId=" + memberId + ", amount=" + amount + "]");
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
        recordUserAction(memberId, action, details);

        // log this to the audit log as well
        String loginfo = action + (details != null ? " " + details : "");
        MsoyServer.flowLog(memberId + (grant ? " G " : " S ") + amount + " " + loginfo);

        // TODO: can we magically get the updated value from the database? stored procedure?
        return loadMemberFlow(memberId);
    }

    /**
     * Records a user action in the database.
     */
    protected void recordUserAction (int memberId, UserAction action, String details)
        throws PersistenceException
    {
        MemberActionLogRecord record = new MemberActionLogRecord();
        record.memberId = memberId;
        record.actionId = action.getNumber();
        record.actionTime = new Timestamp(System.currentTimeMillis());
        record.data = details;
        insert(record);
    }

    // read a game abuse record or create one if needed, possibly also inserting it into the db
    protected GameAbuseRecord getAbuseRecord (int gameId, boolean insertOnCreation)
        throws PersistenceException
    {
        GameAbuseRecord gameRecord = load(GameAbuseRecord.class, gameId);
        if (gameRecord == null) {
            gameRecord = new GameAbuseRecord();
            gameRecord.gameId = gameId;
            gameRecord.abuseFactor = 100;
            gameRecord.accumMinutes = 0;
            if (insertOnCreation) {
                insert(gameRecord);
            }
        }
        return gameRecord;
    }

    /** Minutes in a day. */
    protected static final float DAY_MINS = 24 * 60;
}
