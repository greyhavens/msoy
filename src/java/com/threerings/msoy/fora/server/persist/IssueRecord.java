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
    public static final ColumnExp<Integer> ISSUE_ID = colexp(_R, "issueId");
    public static final ColumnExp<Integer> CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp<Integer> OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp<String> SUMMARY = colexp(_R, "summary");
    public static final ColumnExp<String> DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp<Integer> STATE = colexp(_R, "state");
    public static final ColumnExp<Integer> PRIORITY = colexp(_R, "priority");
    public static final ColumnExp<Integer> TYPE = colexp(_R, "type");
    public static final ColumnExp<Integer> CATEGORY = colexp(_R, "category");
    public static final ColumnExp<Timestamp> CREATED_TIME = colexp(_R, "createdTime");
    public static final ColumnExp<Timestamp> CLOSED_TIME = colexp(_R, "closedTime");
    public static final ColumnExp<String> CLOSE_COMMENT = colexp(_R, "closeComment");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** This issue's unique identifier. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int issueId;

    /** The user id that created the issue. */
    public int creatorId;

    /** The user id that owns the issue. */
    public int ownerId;

    /** The one-line summary of the issue. */
    @Column(length=Issue.MAX_SUMMARY_LENGTH, nullable=true) // TODO: remove post-migration
    public String summary;

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
        issue.summary = summary;
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
        return newKey(_R, issueId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(ISSUE_ID); }
    // AUTO-GENERATED: METHODS END
}
