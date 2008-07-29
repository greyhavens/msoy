//
// $Id$

package com.threerings.msoy.badge.data;

public class EarnedBadge extends Badge
{
    /**
     * When this badge was earned.
     * Long, instead of a long, because we can't stream longs to the ActionScript client.
     */
    public Long whenEarned;

    /** Constructs a new empty EarnedBadge. */
    public EarnedBadge ()
    {
    }

    /** Constructs a new EarnedBadge with the specified type. */
    public EarnedBadge (BadgeType type, long whenEarned)
    {
        super(type);
        this.whenEarned = whenEarned;
    }

    public String toString ()
    {
        return "badgeCode=" + badgeCode + " whenEarned=" + whenEarned;
    }
}
