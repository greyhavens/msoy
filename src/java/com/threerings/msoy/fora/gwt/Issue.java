//
// $Id$

package com.threerings.msoy.fora.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;

/**
 * Contains issue tracking information for bugs and features.
 */
public class Issue
    implements IsSerializable
{
    /** A state indicating the issue is open. */
    public static final int STATE_OPEN = 0;

    /** A state indicating the issue is closed due to resolution. */
    public static final int STATE_RESOLVED = 1;

    /** A state indicating the issue is closed and ignored. */
    public static final int STATE_IGNORED = 2;

    /** A type indicating the issue is a bug. */
    public static final int TYPE_BUG = 0;

    /** A type indicating the issue is a feature request. */
    public static final int TYPE_FEATURE = 1;

    /** A low priority. */
    public static final int PRIORITY_LOW = 0;

    /** A medium priority. */
    public static final int PRIORITY_MEDIUM = 10;

    /** A high priority. */
    public static final int PRIORITY_HIGH = 20;

    /** A category indicating the issue currently uncategorized. */
    public static final int CAT_NONE = 0;

    /** The maximum length of the issue description. */
    public static final int MAX_DESC_LENGTH = 1024;

    /** The maximum length of the closing comment. */
    public static final int MAX_COMMENT_LENGTH = 4096;

    /** Due to lack of enum support in GWT we're forced to do this the hard way. */
    public static final byte[] TYPE_VALUES = new byte[] {
        Issue.TYPE_BUG,
        Issue.TYPE_FEATURE,
    };

    public static final byte[] STATE_VALUES = new byte[] {
        Issue.STATE_OPEN,
        Issue.STATE_RESOLVED,
        Issue.STATE_IGNORED,
    };

    public static final byte[] PRIORITY_VALUES = new byte[] {
        Issue.PRIORITY_LOW,
        Issue.PRIORITY_MEDIUM,
        Issue.PRIORITY_HIGH,
    };

    public static final byte[] CATEGORY_VALUES = new byte[] {
        Issue.CAT_NONE,
    };

    /** This issue's unique identifier. */
    public int issueId;

    /** The user that created the issue. */
    public MemberName creator;

    /** The user that owns the issue. */
    public MemberName owner;

    /** The description of the issue. */
    public String description;

    /** The state of the issue (OPEN, RESOLVED, IGNORED, etc). */
    public int state;

    /** The priority of the issue. */
    public int priority;

    /** The type of issue (BUG, FEATURE). */
    public int type;

    /** The category of the issue. */
    public int category;

    /** The time the issue was created. */
    public Date createdTime;

    /** The time the issue was closed. */
    public Date closedTime;

    /** A comment on the issue when it is closed. */
    public String closeComment;
}
