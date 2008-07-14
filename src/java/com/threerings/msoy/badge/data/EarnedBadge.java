//
// $Id$

package com.threerings.msoy.badge.data;

public class EarnedBadge extends Badge
{
    /** When this badge was earned. */
    public Long whenEarned;

    /** Constructs a new EarnedBadge with the specified type. */
    public EarnedBadge (BadgeType type, Long whenEarned)
    {
        this.badgeCode = type.getCode();
        this.whenEarned = whenEarned;
    }
}
