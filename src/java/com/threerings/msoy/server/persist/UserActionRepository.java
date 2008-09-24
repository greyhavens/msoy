//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.operator.Arithmetic;
import com.samskivert.util.IntIntMap;

import com.threerings.msoy.data.UserAction;
import com.threerings.presents.annotation.BlockingThread;

/**
 * Manages persistent information stored on a per-member basis.
 */
@Singleton @BlockingThread
public class UserActionRepository extends DepotRepository
{
    /**
     * Creates a flow repository for.
     */
    @Inject public UserActionRepository (final PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Logs an action for a member with optional action-specific data.
     */
    public void logUserAction (final UserAction action)
    {
        final MemberActionLogRecord record = new MemberActionLogRecord();
        record.memberId = action.memberId;
        record.actionId = action.type.getNumber();
        record.actionTime = new Timestamp(System.currentTimeMillis());
        record.data = action.data;
        insert(record);
    }

    /**
     * Retrieves the log records for the specified member ID.  Since {@link #assessHumanity(int,
     * int, int)} deletes all of these records for a member, this will only return the records
     * since the last time that method was called.
     *
     * @param memberId ID of the member to retrieve records for.
     * @return Collection of action log records for that member.
     */
    public Collection<MemberActionLogRecord> getLogRecords (final int memberId)
    {
        final Where condition = new Where(MemberActionLogRecord.MEMBER_ID_C, memberId);
        return findAll(MemberActionLogRecord.class, condition);
    }

    /**
     * Assess this member's humanity based on actions taken between the last assessment and now.
     */
    public int assessHumanity (final int memberId, final int currentHumanity,
                               final int secsSinceLast)
    {
        // load up all of their actions since our last humanity assessment
        final Collection<MemberActionLogRecord> records = getLogRecords(memberId);

        // if they've done nothing of note, do not adjust their humanity
        if (records.size() == 0) {
            return currentHumanity;
        }

        // summarize their action counts and compute any humanity adjustment
        final HumanityHelper helper = new HumanityHelper();
        final IntIntMap actionCounts = new IntIntMap();
        for (final MemberActionLogRecord record : records) {
            // note that they performed this action once
            actionCounts.increment(record.actionId, 1);
            // tell our humanity helper about the record
            helper.noteRecord(record);
        }

        final Map<String, SQLExpression> fieldMap = Maps.newHashMap();

        // update their action summary counts
        for (final IntIntMap.IntIntEntry entry : actionCounts.entrySet()) {
            fieldMap.put(
                MemberActionSummaryRecord.COUNT,
                new Arithmetic.Add(MemberActionSummaryRecord.COUNT_C, entry.getIntValue()));
            final int rows = updateLiteral(
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

        // clear their log tables
        deleteAll(MemberActionLogRecord.class,
                  new Where(MemberActionLogRecord.MEMBER_ID_C, memberId));

        // finally compute a new humanity assessment for this member (TODO: load up action summary
        // counts, pass that data in as well)
        return helper.computeNewHumanity(memberId, currentHumanity, secsSinceLast);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (final Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MemberActionLogRecord.class);
        classes.add(MemberActionSummaryRecord.class);
    }
}
