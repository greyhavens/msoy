//
// $Id$

package com.threerings.msoy.badge.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Id;

import com.threerings.msoy.badge.data.EarnedBadge;

public class BadgeRecord extends PersistentRecord
{
    /** The id of the member that holds this badge. */
    @Id
    public int memberId;

    /** The code that uniquely identifies the badge type. */
    @Id
    public int badgeCode;

    /** The date and time when this badge was earned. */
    public Timestamp whenEarned;

    /**
     * Converts this persistent record to a runtime record.
     */
    public EarnedBadge toBadge ()
    {
        EarnedBadge badge = new EarnedBadge();
        badge.badgeCode = badgeCode;
        badge.whenEarned = whenEarned.getTime();

        return badge;
    }
}
