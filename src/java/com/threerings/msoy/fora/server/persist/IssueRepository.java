//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.util.List;
import java.util.Set;

import java.sql.Timestamp;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.CountRecord;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.SQLExpression;

import com.threerings.presents.annotation.BlockingThread;

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
    public List<IssueRecord> loadIssues (Set<Integer> states, int offset, int count)
    {
        return loadIssues(states, 0, offset, count);
    }

    /**
     * Load issues of specific types, states and ownerId.
     */
    public List<IssueRecord> loadIssues (Set<Integer> states, int ownerId, int offset, int count)
    {
        List<SQLExpression> whereBits = Lists.newArrayList();
        whereBits.add(IssueRecord.STATE.in(states));
        if (ownerId > 0) {
            whereBits.add(IssueRecord.OWNER_ID.eq(ownerId));
        }
        OrderBy orderBy;
        if (!states.contains(Issue.STATE_OPEN)) {
            orderBy = OrderBy.descending(IssueRecord.CLOSED_TIME);
        } else {
            orderBy = new OrderBy(
                new SQLExpression[] { IssueRecord.PRIORITY, IssueRecord.CREATED_TIME },
                new OrderBy.Order[] { OrderBy.Order.DESC, OrderBy.Order.DESC });
        }
        return findAll(IssueRecord.class, new Where(Ops.and(whereBits)),
                       new Limit(offset, count), orderBy);
    }

    /**
     * Load an issue from the issueId.
     */
    public IssueRecord loadIssue (int issueId)
    {
        return load(IssueRecord.getKey(issueId));
    }

    /**
     * Load the total number of issues with specific types, states.
     */
    public int loadIssueCount (Set<Integer> states)
    {
        List<SQLExpression> whereBits = Lists.newArrayList();
        if (states != null) {
            whereBits.add(IssueRecord.STATE.in(states));
        }
        return load(CountRecord.class, new FromOverride(IssueRecord.class),
                    new Where(Ops.and(whereBits))).count;
    }

    /**
     * Creates an issue.
     */
    public IssueRecord createIssue (Issue issue)
    {
        IssueRecord ir = new IssueRecord();
        ir.creatorId = issue.creator.getId();
        ir.ownerId = (issue.owner != null ? issue.owner.getId() : -1);
        ir.summary = issue.summary;
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
        updatePartial(IssueRecord.getKey(issue.issueId),
                      IssueRecord.OWNER_ID, (issue.owner != null ? issue.owner.getId() : -1),
                      IssueRecord.SUMMARY, issue.summary,
                      IssueRecord.DESCRIPTION, issue.description,
                      IssueRecord.STATE, issue.state,
                      IssueRecord.PRIORITY, issue.priority,
                      IssueRecord.TYPE, issue.type,
                      IssueRecord.CATEGORY, issue.category,
                      IssueRecord.CLOSE_COMMENT, issue.closeComment,
                      IssueRecord.CLOSED_TIME, (issue.closeComment == null ?
                                                null : new Timestamp(System.currentTimeMillis())));
    }

    /**
     * Reopens an issue with an updated description, clearing the close comment and the
     * close time in the process.
     */
    public void reopenIssue (int issueId, String newDescription)
    {
        updatePartial(IssueRecord.getKey(issueId),
                      IssueRecord.STATE, Issue.STATE_OPEN,
                      IssueRecord.DESCRIPTION, newDescription,
                      IssueRecord.CLOSE_COMMENT, null,
                      IssueRecord.CLOSED_TIME, null);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(IssueRecord.class);
    }
}
