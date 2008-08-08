//
// $Id$

package com.threerings.msoy.badge.data.all;

public class EarnedBadge extends Badge
{
    /** The highest badge level that the player has attained. */
    public int level;

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
    public EarnedBadge (int badgeCode, int level, long whenEarned)
    {
        super(badgeCode);
        this.level = level;
        this.whenEarned = whenEarned;
    }

    public String toString ()
    {
        return "badgeCode=" + badgeCode + " level=" + level + " whenEarned=" + whenEarned;
    }
}
