//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.util.Date;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;

import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;

import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.fora.gwt.Issue;

/**
 * Contains issue tracking information for bugs and features.
 */
public class IssueRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<IssueRecord> _R = IssueRecord.class;
    public static final ColumnExp ISSUE_ID = colexp(_R, "issueId");
    public static final ColumnExp CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp STATE = colexp(_R, "state");
    public static final ColumnExp PRIORITY = colexp(_R, "priority");
    public static final ColumnExp TYPE = colexp(_R, "type");
    public static final ColumnExp CATEGORY = colexp(_R, "category");
    public static final ColumnExp CREATED_TIME = colexp(_R, "createdTime");
    public static final ColumnExp CLOSED_TIME = colexp(_R, "closedTime");
    public static final ColumnExp CLOSE_COMMENT = colexp(_R, "closeComment");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** This issue's unique identifier. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int issueId;

    /** The user id that created the issue. */
    public int creatorId;

    /** The user id that owns the issue. */
    public int ownerId;

    /** The description of the issue. */
    @Column(length=Issue.MAX_DESC_LENGTH)
    public String description;

    /** The state of the issue (OPEN, CLOSED, IGNORED, etc). */
    public int state;

    /** The priority of the issue. */
    public int priority;

    /** The type of issue (BUG, FEATURE). */
    public int type;

    /** The category of the issue. */
    public int category;

    /** The time the issue was created. */
    public Timestamp createdTime;

    /** The time the issue was closed. */
    @Column(nullable=true)
    public Timestamp closedTime;

    /** A comment on the issue when it is closed. */
    @Column(length=Issue.MAX_COMMENT_LENGTH, nullable=true)
    public String closeComment;

    /**
     * Converts this persistent record to a runtime record.
     */
    public Issue toIssue ()
    {
        Issue issue = new Issue();
        issue.issueId = issueId;
        issue.description = description;
        issue.state = state;
        issue.priority = priority;
        issue.type = type;
        issue.category = category;
        issue.createdTime = new Date(createdTime.getTime());
        issue.closedTime = (closedTime == null ? null : new Date(closedTime.getTime()));
        issue.closeComment = closeComment;
        return issue;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link IssueRecord}
     * with the supplied key values.
     */
    public static Key<IssueRecord> getKey (int issueId)
    {
        return new Key<IssueRecord>(
                IssueRecord.class,
                new ColumnExp[] { ISSUE_ID },
                new Comparable[] { issueId });
    }
    // AUTO-GENERATED: METHODS END
}
