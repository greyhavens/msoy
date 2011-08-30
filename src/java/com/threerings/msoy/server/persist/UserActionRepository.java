//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.UserAction;

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
     * Retrieves the log records for the specified member ID.
     *
     * @param memberId ID of the member to retrieve records for.
     * @return Collection of action log records for that member.
     */
    public Collection<MemberActionLogRecord> getLogRecords (final int memberId)
    {
        final Where condition = new Where(MemberActionLogRecord.MEMBER_ID, memberId);
        return findAll(MemberActionLogRecord.class, condition);
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        deleteAll(MemberActionLogRecord.class,
                  new Where(MemberActionLogRecord.MEMBER_ID.in(memberIds)));
        deleteAll(MemberActionSummaryRecord.class,
                  new Where(MemberActionSummaryRecord.MEMBER_ID.in(memberIds)));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (final Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MemberActionLogRecord.class);
        classes.add(MemberActionSummaryRecord.class);
    }
}
