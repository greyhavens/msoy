//
// $Id$

package com.threerings.msoy.badge.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;

/**
 * A database record representing a badge that has been "suppressed" by the user.
 */
public class SuppressedBadgeRecord extends PersistentRecord
{
    /** The id of the member that suppressed this badge. */
    public int memberId;

    /** The code that uniquely identifies the badge type. */
    public int badgeCode;
}
