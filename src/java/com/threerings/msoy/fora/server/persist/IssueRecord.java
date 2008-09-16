//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.util.Date;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;

import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;

import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.fora.gwt.Issue;

/**
 * Contains issue tracking information for bugs and features.
 */
public class IssueRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #issueId} field. */
    public static final String ISSUE_ID = "issueId";

    /** The qualified column identifier for the {@link #issueId} field. */
    public static final ColumnExp ISSUE_ID_C =
        new ColumnExp(IssueRecord.class, ISSUE_ID);

    /** The column identifier for the {@link #creatorId} field. */
    public static final String CREATOR_ID = "creatorId";

    /** The qualified column identifier for the {@link #creatorId} field. */
    public static final ColumnExp CREATOR_ID_C =
        new ColumnExp(IssueRecord.class, CREATOR_ID);

    /** The column identifier for the {@link #ownerId} field. */
    public static final String OWNER_ID = "ownerId";

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(IssueRecord.class, OWNER_ID);

    /** The column identifier for the {@link #description} field. */
    public static final String DESCRIPTION = "description";

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(IssueRecord.class, DESCRIPTION);

    /** The column identifier for the {@link #state} field. */
    public static final String STATE = "state";

    /** The qualified column identifier for the {@link #state} field. */
    public static final ColumnExp STATE_C =
        new ColumnExp(IssueRecord.class, STATE);

    /** The column identifier for the {@link #priority} field. */
    public static final String PRIORITY = "priority";

    /** The qualified column identifier for the {@link #priority} field. */
    public static final ColumnExp PRIORITY_C =
        new ColumnExp(IssueRecord.class, PRIORITY);

    /** The column identifier for the {@link #type} field. */
    public static final String TYPE = "type";

    /** The qualified column identifier for the {@link #type} field. */
    public static final ColumnExp TYPE_C =
        new ColumnExp(IssueRecord.class, TYPE);

    /** The column identifier for the {@link #category} field. */
    public static final String CATEGORY = "category";

    /** The qualified column identifier for the {@link #category} field. */
    public static final ColumnExp CATEGORY_C =
        new ColumnExp(IssueRecord.class, CATEGORY);

    /** The column identifier for the {@link #createdTime} field. */
    public static final String CREATED_TIME = "createdTime";

    /** The qualified column identifier for the {@link #createdTime} field. */
    public static final ColumnExp CREATED_TIME_C =
        new ColumnExp(IssueRecord.class, CREATED_TIME);

    /** The column identifier for the {@link #closedTime} field. */
    public static final String CLOSED_TIME = "closedTime";

    /** The qualified column identifier for the {@link #closedTime} field. */
    public static final ColumnExp CLOSED_TIME_C =
        new ColumnExp(IssueRecord.class, CLOSED_TIME);

    /** The column identifier for the {@link #closeComment} field. */
    public static final String CLOSE_COMMENT = "closeComment";

    /** The qualified column identifier for the {@link #closeComment} field. */
    public static final ColumnExp CLOSE_COMMENT_C =
        new ColumnExp(IssueRecord.class, CLOSE_COMMENT);
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
                new String[] { ISSUE_ID },
                new Comparable[] { issueId });
    }
    // AUTO-GENERATED: METHODS END
}
