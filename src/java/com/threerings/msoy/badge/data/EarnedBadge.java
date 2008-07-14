//
// $Id$

package com.threerings.msoy.badge.data;

public class EarnedBadge extends Badge
{
    /** When this badge was earned. */
    public long whenEarned;

    /** Constructs a new empty EarnedBadge. */
    public EarnedBadge ()
    {
    }

    /** Constructs a new EarnedBadge with the specified type. */
    public EarnedBadge (BadgeType type, long whenEarned)
    {
        this.badgeCode = type.getCode();
        this.whenEarned = whenEarned;
    }
}
