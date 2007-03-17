//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.depot.CacheKey;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistenceContext.CacheListener;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.GroupBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.FunctionExp;
import com.samskivert.jdbc.depot.expression.LiteralExp;
import com.samskivert.jdbc.depot.operator.Logic.*;
import com.samskivert.jdbc.depot.operator.Conditionals.*;

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
                _ctx.cacheInvalidate(new Key<MemberFlowRecord>(
                        MemberFlowRecord.class, MemberFlowRecord.MEMBER_ID, member.memberId));
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
     * @return -1 if no flow was granted as a result of this action, the member's new flow count if
     * flow was granted by the action.
     */
    public int logUserAction (int memberId, UserAction action, String data)
        throws PersistenceException
    {
        MemberActionLogRecord record = new MemberActionLogRecord();
        record.memberId = memberId;
        record.actionId = action.getNumber();
        record.actionTime = new Timestamp(System.currentTimeMillis());
        record.data = data;
        insert(record);

        // if they get flow for performing this action, grant it to them
        if (action.getFlow() > 0) {
            return updateFlow(memberId, action.getFlow(), action.toString() + " " + data, true);
        } else {
            return -1;
        }
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

        if (gameRecord.accumMinutes >= 
                RuntimeConfig.server.abuseFactorReassessment) {
            // load all actions logged since our last assessment
            Collection<GameFlowSummaryRecord> records =
                findAll(GameFlowSummaryRecord.class,
                    new Where(GameFlowGrantLogRecord.GAME_ID, gameId),
                    new FromOverride(GameFlowGrantLogRecord.class),
                    new FieldOverride(GameFlowSummaryRecord.GAME_ID,
                                      GameFlowGrantLogRecord.GAME_ID_C),
                    new FieldOverride(GameFlowSummaryRecord.AMOUNT,
                                      new FunctionExp("sum", GameFlowGrantLogRecord.AMOUNT_C)));

            // write an algorithm that actually does something with 'records' here
            gameRecord.abuseFactor = 123;
            gameRecord.accumMinutes = 0;
            
            // then delete the records
            deleteAll(MemberActionLogRecord.class,
                      new Where(GameFlowGrantLogRecord.GAME_ID, gameId),
                      null);
        }
        store(gameRecord);
    }

    /**
     * <em>Do not use this method!</em> It exists only because we must work with the coin system
     * which tracks members by username rather than id.
     */
    public int grantFlow (String accountName, int actionId, String details, int amount)
        throws PersistenceException
    {
        MemberRecord record =
            load(MemberRecord.class, new Where(MemberRecord.ACCOUNT_NAME, accountName));
        if (record == null) {
            throw new PersistenceException(
                "Unknown member [accountName=" + accountName + ", actionId=" + actionId + "]");
        }
        return updateFlow(record.memberId, amount, details, false);
    }

    /**
     * Expire a member's flow given that dT minute passed since last we did so.  The flow field of
     * the supplied MemberRecord is modified by this method: the expired flow is subtracted from
     * it.
     * 
     * @return the amount of flow that was expired on the backend
     */
    public int expireFlow (MemberRecord record, int dT)
        throws PersistenceException
    {
        float dailyExpiration = RuntimeConfig.server.dailyFlowEvaporation;
        int toExpire = (int) (record.flow * Math.pow(dailyExpiration, DAY_MINS/dT));
        if (toExpire > 0) {
            record.flow -= toExpire;
            update(record);
        }
        return toExpire;
    }

    /**
     * Helper function for {@link #spendFlow} and {@link #grantFlow}.
     *
     * @return the user's new flow value following the update.
     */
    public int updateFlow (int memberId, int amount, String details, boolean grant)
        throws PersistenceException
    {
        String type = (grant ? "grant" : " spend");
        if (amount <= 0) {
            throw new PersistenceException(
                "Illegal flow " + type + " [memberId=" + memberId + ", amount=" + amount + "]");
        }

        String op = grant ? "+" : "-";

        Key key = new Key<MemberRecord>(MemberRecord.class, MemberRecord.MEMBER_ID, memberId);

        int mods = updateLiteral(MemberRecord.class, key, key,
                                 MemberRecord.FLOW, MemberRecord.FLOW + op + amount);
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
            mods = updateLiteral(
                DailyFlowRecord.class,
                new Where(DailyFlowRecord.TYPE, type, DailyFlowRecord.DATE, date),
                null,
                DailyFlowRecord.AMOUNT, DailyFlowRecord.AMOUNT + op + amount);
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

        MsoyServer.flowLog(
            memberId + (grant ? " G " : " S ") + amount + (details != null ? " " + details : ""));

        // TODO: can we magically get the updated value from the database? stored procedure?
        return loadMemberFlow(memberId).flow;
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
