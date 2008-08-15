//
// $Id$

package com.threerings.msoy.badge.data.all;

import com.threerings.msoy.data.all.DeploymentConfig;

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

    @Override // from Badge
    public String imageUrl ()
    {
        return DeploymentConfig.staticMediaURL + BADGE_IMAGE_DIR + Integer.toHexString(badgeCode) +
            "_" + level + "f" + BADGE_IMAGE_TYPE;
    }

    @Override // from Badge
    public boolean equals (Object o)
    {
        if (o instanceof EarnedBadge) {
            EarnedBadge other = (EarnedBadge)o;
            return super.equals(other) && other.level == this.level &&
                ((long)other.whenEarned) == ((long)this.whenEarned);
        }
        return false;
    }

    @Override // from Badge
    public int hashCode ()
    {
        return badgeCode * level + (whenEarned != null ? whenEarned.intValue() : 0);
    }
}
