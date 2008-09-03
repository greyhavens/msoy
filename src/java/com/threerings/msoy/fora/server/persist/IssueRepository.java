//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.util.List;
import java.util.Set;

import java.sql.Timestamp;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntSet;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;

import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;

import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.expression.SQLExpression;

import com.samskivert.jdbc.depot.operator.Conditionals;
import com.samskivert.jdbc.depot.operator.Logic;
import com.samskivert.jdbc.depot.operator.SQLOperator;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.server.persist.CountRecord;

import com.threerings.msoy.fora.gwt.Issue;

/**
 * Manages issues.
 */
@Singleton @BlockingThread
public class IssueRepository extends DepotRepository
{
    @Inject public IssueRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Load issues of specific types and states.
     */
    public List<IssueRecord> loadIssues (IntSet types, IntSet states, int offset, int count)
    {
        return loadIssues(types, states, 0, offset, count);
    }

    /**
     * Load issues of specific types, states and ownerId.
     */
    public List<IssueRecord> loadIssues (IntSet types, IntSet states, int ownerId,
                                         int offset, int count)
    {
        List<SQLOperator> whereBits = Lists.newArrayList();
        ColumnExp sortColumn = IssueRecord.CREATED_TIME_C;
        if (ownerId > 0) {
            whereBits.add(new Conditionals.Equals(IssueRecord.OWNER_ID_C, ownerId));
        }
        if (types != null) {
            whereBits.add(new Conditionals.In(IssueRecord.TYPE_C, types));
        }
        if (states != null) {
            whereBits.add(new Conditionals.In(IssueRecord.STATE_C, states));
            if (!states.contains(Issue.STATE_OPEN)) {
                sortColumn = IssueRecord.CLOSED_TIME_C;
            }
        }
        return findAll(IssueRecord.class,
                       new Where(new Logic.And(whereBits)),
                       new Limit(offset, count),
                       new OrderBy(
                           new SQLExpression[] { IssueRecord.PRIORITY_C, sortColumn },
                           new OrderBy.Order[] { OrderBy.Order.DESC, OrderBy.Order.DESC }));
    }

    /**
     * Load an issue from the issueId.
     */
    public IssueRecord loadIssue (int issueId)
    {
        return load(IssueRecord.class, issueId);
    }

    /**
     * Load the total number of issues with specific types, states.
     */
    public int loadIssueCount (IntSet types, IntSet states)
    {
        List<SQLOperator> whereBits = Lists.newArrayList();
        if (types != null) {
            whereBits.add(new Conditionals.In(IssueRecord.TYPE_C, types));
        }
        if (states != null) {
            whereBits.add(new Conditionals.In(IssueRecord.STATE_C, states));
        }
        return load(CountRecord.class,
                       new FromOverride(IssueRecord.class),
                       new Where(new Logic.And(whereBits))).count;
    }

    /**
     * Creates an issue.
     */
    public IssueRecord createIssue (Issue issue)
    {
        IssueRecord ir = new IssueRecord();
        ir.creatorId = issue.creator.getMemberId();
        ir.ownerId = (issue.owner != null ? issue.owner.getMemberId() : -1);
        ir.description = issue.description;
        ir.state = issue.state;
        ir.priority = issue.priority;
        ir.type = issue.type;
        ir.category = issue.category;
        ir.createdTime = new Timestamp(System.currentTimeMillis());
        insert(ir);
        return ir;
    }

    /**
     * Updates an issue.
     */
    public void updateIssue (Issue issue)
    {
        updatePartial(IssueRecord.class, issue.issueId,
                IssueRecord.OWNER_ID, (issue.owner != null ? issue.owner.getMemberId() : -1),
                IssueRecord.DESCRIPTION, issue.description,
                IssueRecord.STATE, issue.state,
                IssueRecord.PRIORITY, issue.priority,
                IssueRecord.TYPE, issue.type,
                IssueRecord.CATEGORY, issue.category,
                IssueRecord.CLOSE_COMMENT, issue.closeComment,
                IssueRecord.CLOSED_TIME, (issue.closeComment == null ?
                    null : new Timestamp(System.currentTimeMillis())));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(IssueRecord.class);
    }
}
